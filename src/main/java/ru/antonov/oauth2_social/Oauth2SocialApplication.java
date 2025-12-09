package ru.antonov.oauth2_social;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ru.antonov.oauth2_social.common.FileService;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.repository.UserRepository;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class Oauth2SocialApplication implements CommandLineRunner {
	private final UserRepository userRepository;
	private final FileService fileService;
	@Value("${spring.application.file-storage.base-path}")
	private String basePath;

	public static void main(String[] args) {
		SpringApplication.run(Oauth2SocialApplication.class, args);
	}

	// admin 427945e0-1f70-4a12-9c63-2541fff072cc
	// i d8058e12-1ef2-4e98-b2a5-9b1d12fe80fb


	@Override
	public void run(String... args) throws Exception {
		createAdmin();
		createCatalogue();
	}

	public void createAdmin(){
		if(!userRepository.existsByEmail("example@gmail.com")) {
			User user = User.builder()
					.age(21)
					.name("Елена")
					.surname("Фонталина")
					.patronymic("Сергеевна")
					.email("example@gmail.com")
					.isTfaEnabled(false)
					.isEnabled(true)
					.role(Role.ADMIN)
					.password("{noop}abcd12345678")
					.build();
			userRepository.save(user);
		}
	}

	public void createCatalogue(){
		Path path = Paths.get(basePath);
		fileService.createDirectory(path);
	}
}
