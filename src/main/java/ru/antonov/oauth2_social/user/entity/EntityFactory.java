package ru.antonov.oauth2_social.user.entity;


import ru.antonov.oauth2_social.user.dto.GroupCreateRequestDto;
import ru.antonov.oauth2_social.user.dto.InstitutionCreateRequestDto;
import ru.antonov.oauth2_social.user.dto.UserCreateRequestDto;

public class EntityFactory {

    public static User makeUserEntity(
            UserCreateRequestDto dto, String encodedPassword, Institution institution, Group group
    ){

        // todo ИСПРАВИТЬ isEnabled(false)
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
                .isEnabled(true)
                .isTfaEnabled(false)
                .tfaSecret(null)
                .build();
    }

    public static Institution makeInstitutionEntity(InstitutionCreateRequestDto dto){
        return Institution
                .builder()
                .type(dto.getInstitutionType())
                .fullName(dto.getFullName())
                .shortName(dto.getShortName())
                .location(dto.getLocation())
                .email(dto.getEmail())
                .build();
    }

    public static Group makeGroupEntity(GroupCreateRequestDto dto, Institution institution){
        return Group.builder()
                .name(dto.getName())
                .institution(institution)
                .build();
    }
}
