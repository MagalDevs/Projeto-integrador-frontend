package com.example.projeto_integrador.network;

/**
 * Configuração centralizada da API.
 * <p>
 * Todas as URLs e paths ficam aqui para facilitar a manutenção.
 * Ao trocar de ambiente (local → produção), basta alterar {@link #BASE_URL}.
 */
public final class ApiConfig {

    private ApiConfig() { /* utility class */ }

    // ── Base ─────────────────────────────────────────────────────────────────
    public static final String BASE_URL = "http://10.64.22.159:8080";

    // ── Usuários ─────────────────────────────────────────────────────────────
    public static final String USUARIOS          = "/usuarios";
    public static final String USUARIOS_ADMIN    = "/usuarios/admin";

    // ── Denúncias ────────────────────────────────────────────────────────────
    public static final String DENUNCIAS         = "/denuncias";
    public static final String DENUNCIAS_ADMIN   = "/denuncias/admin";

    /** Uso: DENUNCIAS_POR_USUARIO + "/{usuarioId}" */
    public static final String DENUNCIAS_POR_USUARIO = "/denuncias/usuario";

    /** Uso: DENUNCIAS + "/{id}/update-status/{status}" */
    public static final String DENUNCIAS_UPDATE_STATUS = "/update-status";

    // ── Categorias ───────────────────────────────────────────────────────────
    public static final String CATEGORIAS        = "/categorias";
    public static final String CATEGORIAS_ADMIN  = "/categorias/admin";

    // ── Login (placeholder — ajustar quando endpoint estiver pronto) ─────────
    public static final String LOGIN             = "/login";

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Monta a URL completa para um dado path.
     *
     * @param path ex.: "/usuarios" ou "/denuncias/admin"
     * @return URL completa (ex.: "http://10.64.22.159:8080/usuarios")
     */
    public static String url(String path) {
        return BASE_URL + path;
    }
}
