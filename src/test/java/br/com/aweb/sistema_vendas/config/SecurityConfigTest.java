package br.com.aweb.sistema_vendas.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.aweb.sistema_vendas.controller.ClienteController;
import br.com.aweb.sistema_vendas.controller.HomeController;
import br.com.aweb.sistema_vendas.service.ClienteService;

@WebMvcTest({ HomeController.class, ClienteController.class })
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.usuarios.admin.email=admin@teste.com",
        "app.usuarios.admin.senha=senhaAdmin",
        "app.usuarios.padrao.email=padrao@teste.com",
        "app.usuarios.padrao.senha=senhaPadrao"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClienteService clienteService;

    @Test
    void semLoginRedirecionaParaTelaDeLogin() throws Exception {
        mvc.perform(get("/clientes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void telaDeLoginEhPublica() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void loginDoAdminComSenhaCorreta() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                .user("admin@teste.com").password("senhaAdmin"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginDoPadraoComSenhaCorreta() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                .user("padrao@teste.com").password("senhaPadrao"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginComSenhaErrada() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                .user("admin@teste.com").password("errada"))
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    void padraoPodeListarClientes() throws Exception {
        mvc.perform(get("/clientes").with(user("padrao@teste.com").roles("PADRAO")))
                .andExpect(status().isOk());
    }

    @Test
    void padraoNaoPodeCadastrarCliente() throws Exception {
        mvc.perform(get("/clientes/novo").with(user("padrao@teste.com").roles("PADRAO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPodeCadastrarCliente() throws Exception {
        mvc.perform(get("/clientes/novo").with(user("admin@teste.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioLogadoAcessaHome() throws Exception {
        mvc.perform(get("/").with(user("padrao@teste.com").roles("PADRAO")))
                .andExpect(status().isOk());
    }
}
