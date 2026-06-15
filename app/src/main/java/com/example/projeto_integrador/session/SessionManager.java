package com.example.projeto_integrador.session;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gerencia a sessão do usuário usando {@link SharedPreferences}.
 * <p>
 * Armazena id, nome, email e role do usuário logado.
 * Permite verificar se o usuário está logado e qual é sua role
 * sem precisar consultar o backend a cada vez.
 */
public final class SessionManager {

    private static final String PREF_NAME    = "cidadao_alerta_session";
    private static final String KEY_LOGGED   = "is_logged_in";
    private static final String KEY_USER_ID  = "user_id";
    private static final String KEY_NAME     = "user_name";
    private static final String KEY_EMAIL    = "user_email";
    private static final String KEY_ROLE     = "user_role";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Salvar / Limpar ──────────────────────────────────────────────────────

    /**
     * Salva os dados do usuário na sessão após login/cadastro bem-sucedido.
     *
     * @param id    UUID do usuário
     * @param nome  nome completo
     * @param email e-mail
     * @param role  "USER" ou "ADMIN"
     */
    public void saveUser(String id, String nome, String email, String role) {
        prefs.edit()
             .putBoolean(KEY_LOGGED, true)
             .putString(KEY_USER_ID, id)
             .putString(KEY_NAME, nome)
             .putString(KEY_EMAIL, email)
             .putString(KEY_ROLE, role)
             .apply();
    }

    /**
     * Encerra a sessão, removendo todos os dados armazenados.
     */
    public void logout() {
        prefs.edit().clear().apply();
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED, false);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_NAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getUserRole() {
        return prefs.getString(KEY_ROLE, "USER");
    }

    /**
     * @return {@code true} se o usuário logado tem role ADMIN.
     */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getUserRole());
    }
}
