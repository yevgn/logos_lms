package ru.antonov.oauth2_social.user.dto;

import ru.antonov.oauth2_social.user.entity.Group;
import ru.antonov.oauth2_social.user.entity.Institution;
import ru.antonov.oauth2_social.user.entity.User;

public class DtoFactory {
    public static UserResponseDto makeUserResponseDto(User user){
        return UserResponseDto
                .builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .surname(user.getSurname())
                .patronymic( user.getPatronymic())
                .role(user.getRole())
                .password(user.getPassword().substring(6))
                .age(user.getAge())
                .group( user.getGroup() == null ? null : user.getGroup().getName())
                .isEnabled(user.isEnabled())
                .institution(user.getInstitution() != null ? makeInstitutionShortResponseDto(user.getInstitution()) : null)
                .build();
    }

    public static InstitutionResponseDto makeInstitutionResponseDto(Institution institution){
        return InstitutionResponseDto
                .builder()
                .id(institution.getId())
                .fullName(institution.getFullName())
                .shortName(institution.getShortName())
                .location(institution.getLocation())
                .institutionType(institution.getType())
                .email(institution.getEmail())
                .build();
    }

    public static GroupResponseDto makeGroupResponseDto(Group group){
        return GroupResponseDto
                .builder()
                .id(group.getId())
                .name(group.getName())
                .institution(makeInstitutionShortResponseDto(group.getInstitution()))
                .build();
    }

    public static UserShortResponseDto makeUserShortResponseDto(User user){
        return UserShortResponseDto
                .builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .patronymic(user.getPatronymic())
                .group(user.getGroup() == null ? null : user.getGroup().getName())
                .role(user.getRole())
                .build();
    }

    public static InstitutionShortResponseDto makeInstitutionShortResponseDto(Institution institution){
        return InstitutionShortResponseDto
                .builder()
                .id(institution.getId())
                .type(institution.getType())
                .shortName(institution.getShortName())
                .build();
    }
}
