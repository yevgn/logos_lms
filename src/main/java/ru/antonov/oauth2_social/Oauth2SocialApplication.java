package ru.antonov.oauth2_social;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.repository.UserRepository;

@SpringBootApplication
@RequiredArgsConstructor
public class Oauth2SocialApplication implements CommandLineRunner {
	private final UserRepository userRepository;

	public static void main(String[] args) {
		SpringApplication.run(Oauth2SocialApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
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
}
