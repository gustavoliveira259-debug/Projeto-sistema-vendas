package br.com.aweb.sistema_vendas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Arquivos estáticos e tela de login são públicos
                .requestMatchers("/css/**", "/js/**", "/login").permitAll()
                // Usuário padrão só pode consultar clientes e produtos
                .requestMatchers(HttpMethod.GET, "/clientes", "/produtos").authenticated()
                // Cadastro, edição e exclusão de clientes e produtos só para ADMIN
                .requestMatchers("/clientes/**", "/produtos/**").hasRole("ADMIN")
                // Demais rotas (home e pedidos) para qualquer usuário logado
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("senha")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?erro")
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Usuários definidos pelas variáveis de ambiente ADMIN_* e PADRAO_*
    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${app.usuarios.admin.email}") String adminEmail,
            @Value("${app.usuarios.admin.senha}") String adminSenha,
            @Value("${app.usuarios.padrao.email}") String padraoEmail,
            @Value("${app.usuarios.padrao.senha}") String padraoSenha) {

        var admin = User.withUsername(adminEmail)
                .password(passwordEncoder.encode(adminSenha))
                .roles("ADMIN")
                .build();

        var padrao = User.withUsername(padraoEmail)
                .password(passwordEncoder.encode(padraoSenha))
                .roles("PADRAO")
                .build();

        return new InMemoryUserDetailsManager(admin, padrao);
    }
}
