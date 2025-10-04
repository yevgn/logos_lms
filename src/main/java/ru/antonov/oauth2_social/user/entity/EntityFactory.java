package ru.antonov.oauth2_social.user.entity;


import ru.antonov.oauth2_social.user.dto.GroupCreateRequestDto;
import ru.antonov.oauth2_social.user.dto.UserCreateRequestDto;

public class EntityFactory {

    public static User makeUserEntity(
            UserCreateRequestDto dto, String encodedPassword, Institution institution, Group group
    ){
        return User.builder()
                .name(dto.getName())
                .surname(dto.getSurname())
                .patronymic(dto.getPatronymic())
                .email(dto.getEmail())
                .age(dto.getAge())
                .role(dto.getRole())
                .password(encodedPassword)
                .institution(institution)
                .group(group)
                .isEnabled(false)
                .isTfaEnabled(false)
                .tfaSecret(null)
                .build();
    }

    public static Group makeGroupEntity(GroupCreateRequestDto dto, Institution institution){
        return Group.builder()
                .name(dto.getName())
                .institution(institution)
                .build();
    }
}
