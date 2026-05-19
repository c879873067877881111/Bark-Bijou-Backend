package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.dto.GoogleProfile;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.exception.ConcurrentOAuthRegistrationException;
import com.smallnine.apiserver.service.FileStorageService;
import com.smallnine.apiserver.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    private static final String DEFAULT_AVATAR = "/member/member_images/user-img.svg";

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Override
    public String updateProfile(User user, String username, String realname, String email,
                                String birthDate, String gender, String phone, MultipartFile avatar) {
        if (username != null && !username.isBlank()) user.setUsername(username);
        if (realname != null) user.setRealname(realname);
        if (email != null && !email.isBlank()) user.setEmail(email);
        if (gender != null) {
            try {
                user.setGender(User.Gender.valueOf(gender));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "性別值不合法，允許值: male, female");
            }
        }
        if (phone != null) user.setPhone(phone);
        if (birthDate != null && !birthDate.isEmpty()) {
            user.setBirthDate(java.time.LocalDate.parse(birthDate));
        }

        if (avatar != null && !avatar.isEmpty()) {
            String storedPath = fileStorageService.store(avatar, "member_images");
            String filename = storedPath.substring(storedPath.lastIndexOf('/') + 1);
            user.setImageUrl("/member/member_images/" + filename);
        }

        userDao.updateProfile(user);
        return user.getImageUrl();
    }

    @Override
    public void changePassword(Long memberId, Long authenticatedUserId, String currentPassword, String newPassword) {
        if (!authenticatedUserId.equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "無權限修改他人密碼");
        }

        User user = userDao.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ResponseCode.INVALID_PASSWORD, "舊密碼錯誤");
        }

        if (newPassword.length() < 6) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "密碼長度至少6位");
        }

        userDao.updatePassword(memberId, passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public User findOrCreateGoogleUser(GoogleProfile profile) {
        if (profile == null || profile.getSub() == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Google profile 缺少 sub");
        }

        Optional<User> byGoogle = userDao.findByGoogleUid(profile.getSub());
        if (byGoogle.isPresent()) {
            return byGoogle.get();
        }

        // Google 通常會給 email（scope 已要 email + openid），但 user 在 consent screen
        // 拒絕分享時會拿到 null。member 表 email 是 NOT NULL UNIQUE，沒 email 無法建帳號。
        if (profile.getEmail() == null || profile.getEmail().isBlank()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST,
                    "Google 帳號未提供 email，無法登入。請重新授權並允許分享 email。");
        }

        Optional<User> byEmail = userDao.findByEmail(profile.getEmail());
        if (byEmail.isPresent()) {
            User user = byEmail.get();

            // 拒絕 auto-link 到 emailValidated=false 的帳號:
            // Google 證明了「現在誰擁有此 email」,但證不到「原本帳號註冊時是否同一人」。
            // 若原帳號從未驗證 email,一旦該 email 落入他人手中(無論本來如何),
            // 對方就能透過 OAuth 接管含密碼的原帳號。要求原帳號先完成 email 驗證。
            if (!Boolean.TRUE.equals(user.getEmailValidated())) {
                log.warn("action=oauth2_link result=denied reason=email_not_validated email={}",
                        user.getEmail());
                throw new BusinessException(ResponseCode.BAD_REQUEST,
                        "此 email 已被註冊但尚未驗證,請先以原註冊方式登入完成 email 驗證後再綁定 Google");
            }

            user.setGoogleUid(profile.getSub());
            user.setGoogleName(profile.getName());
            if (profile.getPicture() != null) {
                user.setImageUrl(profile.getPicture());
            }
            userDao.update(user);
            log.info("action=oauth2_link user={} email={}", user.getUsername(), user.getEmail());
            return user;
        }

        User newUser = buildGoogleUser(profile);
        try {
            userDao.insert(newUser);
            log.info("action=oauth2_register user={} email={}", newUser.getUsername(), newUser.getEmail());
            return newUser;
        } catch (DataIntegrityViolationException e) {
            // 並行情境：另一個 thread 已先 INSERT 同一個 google_uid。
            // 不能在這裡 re-fetch—— PG 已把當前 txn 標為 ABORTED，後續任何 SELECT 都會被拒。
            // 改成拋 ConcurrentOAuthRegistrationException 讓 @Transactional 正常 rollback，
            // 由呼叫端用獨立 transaction (findByGoogleUid) 取回先寫入者。
            log.warn("action=oauth2_register race_condition sub={} reason={}",
                    profile.getSub(), e.getMessage());
            throw new ConcurrentOAuthRegistrationException(profile.getSub(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByGoogleUid(String googleUid) {
        if (googleUid == null || googleUid.isBlank()) {
            return Optional.empty();
        }
        return userDao.findByGoogleUid(googleUid);
    }

    private User buildGoogleUser(GoogleProfile profile) {
        // 用隨機 UUID 片段組 username，避免「google_<sub>」剛好被人手動註冊用走時撞 username UNIQUE
        // 而走錯誤的 race recovery 分支（race recovery 是用 google_uid 找，找不到會 IllegalStateException）。
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User user = new User();
        user.setUsername("google_" + randomSuffix);
        user.setEmail(profile.getEmail());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRealname(profile.getName() != null ? profile.getName() : "Google User");
        user.setGoogleUid(profile.getSub());
        user.setGoogleName(profile.getName());
        user.setImageUrl(profile.getPicture() != null ? profile.getPicture() : DEFAULT_AVATAR);
        user.setEmailValidated(true);
        return user;
    }
}
