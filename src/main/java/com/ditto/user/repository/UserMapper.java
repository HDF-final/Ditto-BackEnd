package com.ditto.user.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("SELECT COUNT(*) FROM app_user WHERE email = #{email}")
    int countByEmail(String email);

    @Select("SELECT COUNT(*) FROM app_user WHERE nickname = #{nickname}")
    int countByNickname(String nickname);

    @Insert("""
            INSERT INTO app_user (
                country_id,
                name,
                email,
                nickname,
                phone,
                password_hash,
                address_bcode,
                jibun_address,
                road_address,
                address_detail,
                role,
                preferred_language_code,
                status,
                created_at
            ) VALUES (
                #{countryId},
                #{name},
                #{email},
                #{nickname},
                #{phone},
                #{passwordHash},
                #{addressBcode},
                #{jibunAddress},
                #{roadAddress},
                #{addressDetail},
                #{role},
                #{preferredLanguageCode},
                #{status},
                SYSTIMESTAMP
            )
            """)
    int insert(SignupUserCommand command);

    record SignupUserCommand(
            Long countryId,
            String name,
            String email,
            String nickname,
            String phone,
            String passwordHash,
            String addressBcode,
            String jibunAddress,
            String roadAddress,
            String addressDetail,
            String role,
            String preferredLanguageCode,
            String status) {
    }
}
