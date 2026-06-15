# 📡 API Routes — Projeto Integrador Backend

> **Base URL:** `http://localhost:8080`
> **Swagger UI:** `http://localhost:8080/swagger-ui.html`

---

## Enums

### `StatusEnum`
| Valor | Descrição |
|-------|-----------|
| `ABERTA` | Aberta |
| `EM_ANALISE` | Em análise |
| `RESOLVIDA` | Resolvida |

### `Role`
| Valor |
|-------|
| `ADMIN` |
| `USER` |

---

## 👥 Usuários (`/usuarios`)

### `GET /usuarios/admin`
Lista todos os usuários.

**Response** `200 OK` — `List<UsuarioDto>`

```json
[
  {
    "id": "uuid",
    "nome": "João Silva",
    "email": "joao@email.com",
    "role": "USER"
  }
]
```

---

### `POST /usuarios`
Cria um novo usuário.

**Content-Type:** `application/json`

**Request Body** — `UsuarioRequestDto`

| Campo      | Tipo     | Obrigatório | Descrição          |
|------------|----------|-------------|--------------------|
| `email`    | `String` | ✅           | Email do usuário   |
| `password` | `String` | ✅           | Senha do usuário   |
| `nome`     | `String` | ✅           | Nome do usuário    |

```json
{
  "email": "joao@email.com",
  "password": "123456",
  "nome": "João Silva"
}
```

**Response** `200 OK` — `UsuarioDto`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "nome": "João Silva",
  "email": "joao@email.com",
  "role": "USER"
}
```

---

### `PATCH /usuarios/{id}`
Atualiza um usuário existente.

**Path Params**

| Param | Tipo     | Descrição       |
|-------|----------|-----------------|
| `id`  | `String` (UUID) | ID do usuário   |

**Content-Type:** `application/json`

**Request Body** — `UsuarioRequestDto`

| Campo      | Tipo     | Obrigatório | Descrição          |
|------------|----------|-------------|--------------------|
| `email`    | `String` | ✅           | Email do usuário   |
| `password` | `String` | ✅           | Senha do usuário   |
| `nome`     | `String` | ✅           | Nome do usuário    |

```json
{
  "email": "novo@email.com",
  "password": "novaSenha",
  "nome": "João Atualizado"
}
```

**Response** `200 OK` — `UsuarioDto`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "nome": "João Atualizado",
  "email": "novo@email.com",
  "role": "USER"
}
```

---

## 📢 Denúncias (`/denuncias`)

### `GET /denuncias/admin`
Lista todas as denúncias (rota admin).

**Response** `200 OK` — `List<DenunciaResponseDto>`

```json
[
  {
    "id": "uuid",
    "titulo": "Buraco na rua",
    "descricao": "Buraco grande na Av. Principal",
    "status": "ABERTA",
    "latitude": -23.5505,
    "longitude": -46.6333,
    "createdAt": "2026-06-01T12:00:00Z",
    "usuario": {
      "id": "uuid",
      "nome": "João Silva",
      "role": "USER"
    },
    "categoria": {
      "id": "uuid",
      "nome": "Infraestrutura"
    },
    "imagens": [
      "https://shmnyiwvnykooywizcjv.supabase.co/storage/v1/object/public/denuncias-imagens/foto1.jpg"
    ]
  }
]
```

---

### `GET /denuncias/{id}`
Busca uma denúncia pelo ID.

**Path Params**

| Param | Tipo     | Descrição           |
|-------|----------|---------------------|
| `id`  | `String` (UUID) | ID da denúncia |

**Response** `200 OK` — `DenunciaResponseDto`

```json
{
  "id": "uuid",
  "titulo": "Buraco na rua",
  "descricao": "Buraco grande na Av. Principal",
  "status": "ABERTA",
  "latitude": -23.5505,
  "longitude": -46.6333,
  "createdAt": "2026-06-01T12:00:00Z",
  "usuario": {
    "id": "uuid",
    "nome": "João Silva",
    "role": "USER"
  },
  "categoria": {
    "id": "uuid",
    "nome": "Infraestrutura"
  },
  "imagens": ["url1", "url2"]
}
```

---

### `GET /denuncias/usuario/{usuarioId}`
Lista todas as denúncias de um usuário específico.

**Path Params**

| Param       | Tipo     | Descrição          |
|-------------|----------|--------------------|
| `usuarioId` | `String` (UUID) | ID do usuário |

**Response** `200 OK` — `List<DenunciaResponseDto>`

*(mesmo formato da listagem acima)*

---

### `POST /denuncias`
Cria uma nova denúncia.

> [!IMPORTANT]
> Esta rota usa **`multipart/form-data`**, NÃO JSON!

**Content-Type:** `multipart/form-data`

**Form Params**

| Campo         | Tipo                  | Obrigatório | Descrição                    |
|---------------|-----------------------|-------------|------------------------------|
| `titulo`      | `String`              | ✅           | Título da denúncia           |
| `descricao`   | `String`              | ✅           | Descrição detalhada          |
| `latitude`    | `String` (float)      | ✅           | Latitude da localização      |
| `longitude`   | `String` (float)      | ✅           | Longitude da localização     |
| `usuarioId`   | `UUID`                | ✅           | ID do usuário que denuncia   |
| `categoriaId` | `UUID`                | ✅           | ID da categoria              |
| `imagens`     | `List<MultipartFile>` | ❌           | Imagens anexadas (arquivos)  |

**Exemplo com `fetch` (JavaScript/Android):**

```javascript
const formData = new FormData();
formData.append("titulo", "Buraco na rua");
formData.append("descricao", "Buraco grande na Av. Principal");
formData.append("latitude", "-23.5505");
formData.append("longitude", "-46.6333");
formData.append("usuarioId", "550e8400-e29b-41d4-a716-446655440000");
formData.append("categoriaId", "660e8400-e29b-41d4-a716-446655440000");
formData.append("imagens", imageFile1);
formData.append("imagens", imageFile2);

fetch("http://localhost:8080/denuncias", {
  method: "POST",
  body: formData
  // NÃO definir Content-Type, o browser adiciona automaticamente com boundary
});
```

**Response** `201 Created` — `Denuncia` (entidade completa)

---

### `PATCH /denuncias/{id}/update-status/{status}`
Atualiza o status de uma denúncia.

**Path Params**

| Param    | Tipo     | Valores aceitos                       | Descrição             |
|----------|----------|---------------------------------------|-----------------------|
| `id`     | `String` (UUID) | —                              | ID da denúncia        |
| `status` | `String` | `ABERTA`, `EM_ANALISE`, `RESOLVIDA`   | Novo status           |

**Exemplo:**
```
PATCH /denuncias/550e8400-e29b-41d4-a716-446655440000/update-status/EM_ANALISE
```

**Response** `200 OK` — `DenunciaResponseDto`

---

## 🗂️ Categorias (`/categorias`)

### `GET /categorias`
Lista todas as categorias.

**Response** `200 OK` — `List<Categoria>`

```json
[
  {
    "id": "uuid",
    "nome": "Infraestrutura"
  }
]
```

---

### `POST /categorias/admin`
Cria uma nova categoria (rota admin).

**Content-Type:** `application/json`

**Request Body** — `CategoriaRequestCreateDto`

| Campo  | Tipo     | Obrigatório | Descrição            |
|--------|----------|-------------|----------------------|
| `nome` | `String` | ✅           | Nome da categoria    |

```json
{
  "nome": "Iluminação"
}
```

**Response** `200 OK` — `Categoria`

```json
{
  "id": "770e8400-e29b-41d4-a716-446655440000",
  "nome": "Iluminação"
}
```

---

### `PATCH /categorias/{id}`
Atualiza uma categoria existente.

**Path Params**

| Param | Tipo     | Descrição          |
|-------|----------|--------------------|
| `id`  | `String` (UUID) | ID da categoria |

**Content-Type:** `application/json`

**Request Body** — `CategoriaRequestCreateDto`

| Campo  | Tipo     | Obrigatório | Descrição            |
|--------|----------|-------------|----------------------|
| `nome` | `String` | ✅           | Novo nome            |

```json
{
  "nome": "Nome Atualizado"
}
```

**Response** `200 OK` — `Categoria`

---

## 📋 Resumo Rápido

| Método    | Rota                                         | Content-Type          | Descrição                        |
|-----------|----------------------------------------------|-----------------------|----------------------------------|
| `GET`     | `/usuarios/admin`                            | —                     | Lista usuários                   |
| `POST`    | `/usuarios`                                  | `application/json`    | Criar usuário                    |
| `PATCH`   | `/usuarios/{id}`                             | `application/json`    | Atualizar usuário                |
| `GET`     | `/denuncias/admin`                           | —                     | Lista todas as denúncias         |
| `GET`     | `/denuncias/{id}`                            | —                     | Buscar denúncia por ID           |
| `GET`     | `/denuncias/usuario/{usuarioId}`             | —                     | Denúncias de um usuário          |
| `POST`    | `/denuncias`                                 | `multipart/form-data` | Criar denúncia (com imagens)     |
| `PATCH`   | `/denuncias/{id}/update-status/{status}`     | —                     | Atualizar status da denúncia     |
| `GET`     | `/categorias`                                | —                     | Lista categorias                 |
| `POST`    | `/categorias/admin`                          | `application/json`    | Criar categoria                  |
| `PATCH`   | `/categorias/{id}`                           | `application/json`    | Atualizar categoria              |
