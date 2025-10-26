package ru.antonov.oauth2_social;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.repository.UserRepository;

@SpringBootApplication
@RequiredArgsConstructor
public class Oauth2SocialApplication implements CommandLineRunner {
	private final UserRepository userRepository;
	//private final PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(Oauth2SocialApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
//		User user = User.builder()
//				.age(21)
//				.name("Евгений")
//				.surname("Антонов")
//				.patronymic("Федорович")
//				.email("zhenya041010@gmail.com")
//				.isTfaEnabled(false)
//				.isEnabled(true)
//				.role(Role.ADMIN)
//				.password(passwordEncoder.encode("z20041010"))
//				.build();
//		userRepository.save(user);
	}
}
