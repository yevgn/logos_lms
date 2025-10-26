package ru.antonov.oauth2_social.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.user.dto.DtoFactory;
import ru.antonov.oauth2_social.user.dto.InstitutionCreateRequestDto;
import ru.antonov.oauth2_social.user.dto.InstitutionResponseDto;
import ru.antonov.oauth2_social.user.entity.EntityFactory;
import ru.antonov.oauth2_social.user.entity.Institution;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.repository.InstitutionRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstitutionService {
    private final InstitutionRepository institutionRepository;
    private final UserService userService;

    @Transactional(rollbackFor = Exception.class)
    public InstitutionResponseDto saveInstitution(User principal, InstitutionCreateRequestDto request){
        Institution institution = EntityFactory.makeInstitutionEntity(request);

        save(institution);

        principal.setInstitution(institution);
        userService.save(principal);

        return DtoFactory.makeInstitutionResponseDto(institution);
    }

    public InstitutionResponseDto findInstitutionById(UUID institutionId){
        Institution institution = findById(institutionId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Такого учебного заведения не существует",
                        String.format(
                                "Ошибка при поиске учебного заведения. Учебного заведения с %s не существует",
                                institutionId
                        )
                ));

        return DtoFactory.makeInstitutionResponseDto(institution);

    }

    public Institution save(Institution institution){
        return institutionRepository.saveAndFlush(institution);
    }

    public Optional<Institution> findById(UUID institutionId){
        return institutionRepository.findById(institutionId);
    }
}
