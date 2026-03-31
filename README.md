tcf5-health-record-transformer
=============================

Resumo
------
Este repositório contém a aplicação "tcf5-health-record-transformer" — um serviço Spring Boot que transforma registros brutos (Kafka) aplicando regras JOLT e expõe uma API REST para criar/consultar/atualizar/excluir MappingRules.

O que está incluído
- Endpoints REST CRUD em /mapping-rules
- Validação básica do JOLT spec (JSON bem formado)
- Persistência em PostgreSQL
- Consumer Kafka configurado para Redpanda (embutido via Docker Compose)
- Collection Postman para testes (postman/MappingRule.postman_collection.json)

Pré-requisitos
--------------
- Java 21
- Docker e Docker Compose
- Maven (geralmente basta usar o wrapper: mvnw/mvnw.cmd)
- (Opcional) Postman para importar a collection

Executando localmente (Maven)
-----------------------------
1) Rodar os testes unitários:

```powershell
# Na raiz do projeto
.\mvnw.cmd -DskipTests=false test
```

2) Empacotar o jar:

```powershell
.\mvnw.cmd -DskipTests package
```

3) Executar diretamente (após build):

```powershell
java -jar .\target\tcf5-health-record-transformer-0.0.1-SNAPSHOT.jar
```

Executando com Docker Compose
-----------------------------
O projeto inclui um `docker-compose.yml` que sobe Postgres, Redpanda (Kafka) e a aplicação.

1) Subir o stack (rebuild da imagem da app):

```powershell
docker compose down
docker compose up -d --build
```

2) Verificar containers e portas:

```powershell
docker compose ps
```

3) Ver logs da aplicação:

```powershell
docker compose logs -f app
```

A app está configurada para escutar em http://localhost:8083 por padrão (definido em application.yaml). Ajuste `baseUrl` no Postman se necessário.

Variáveis de ambiente importantes
---------------------------------
No `docker-compose.yml` e `application.yaml` são usados placeholders que podem ser substituídos por variáveis de ambiente:
- DB_HOST (ex: postgres)
- DB_NAME (ex: sus_interop)
- DB_USER (ex: sus_admin)
- DB_PASS (ex: sus_password)
- KAFKA_BOOTSTRAP_SERVERS (ex: redpanda:9092)
- SERVER_PORT / SERVER_ADDRESS (aplicação)

Se preferir iniciar localmente sem Docker, configure as mesmas variáveis no ambiente ou em `application.yaml`.

Testando a API (curl)
---------------------
Exemplo: listar todas as mapping rules (GET):

```powershell
curl.exe -v http://127.0.0.1:8083/mapping-rules
```

Exemplo: criar uma MappingRule (POST) — observe que `joltSpec` deve ser JSON válido (array ou objeto):

```powershell
curl.exe -v -X POST http://127.0.0.1:8083/mapping-rules -H "Content-Type: application/json" -d '{"clientId":"MY_CLIENT","joltSpec":[{"op":"shift","spec":{"c":"C"}}]}'
```

Resposta esperada: 201 Created (Location header) ou 200 OK (se upsert/overwrite).

Usando a Collection do Postman
-----------------------------
1) Importe `postman/MappingRule.postman_collection.json` e `postman/MappingRule.postman_environment.json` no Postman.
2) Atualize a variável de ambiente `baseUrl` para `http://localhost:8083` se necessário.
3) Execute as requests:
   - Create MappingRule (valid)
   - List MappingRules
   - Get/Put/Delete usando a {{clientId}} gerada automaticamente pelo script de teste na collection.

Observações sobre o payload `joltSpec`
-------------------------------------
- Envie `joltSpec` como JSON (array/obj) — não como string escapada. O DTO aceita tanto string quanto estrutura JSON e converte internamente.
- Exemplos de joltSpec podem ser encontrados na pasta `HELP.md` (ou no código de testes). Se precisar de mais exemplos, posso adicionar.

Depuração / troubleshooting
--------------------------
- Se receber `ECONNREFUSED` no host: verifique `docker compose ps` e `docker compose logs app`.
- Se receber `Empty reply from server`: cheque os logs do app para ver se a requisição foi recebida e se ocorreu alguma exceção.
- Se a app não consegue conectar ao Postgres: verifique variáveis DB_* e o health do container Postgres.
- Se o Kafka consumer não conectar: verifique `KAFKA_BOOTSTRAP_SERVERS` (dentro do compose deve apontar para `redpanda:9092`).

Cobertura de testes
-------------------
- JaCoCo foi configurado no `pom.xml` e está excluindo o package `br.com.tcf5_health_record_transformer.config` do relatório de cobertura.

Próximos passos sugeridos
------------------------
- Adicionar healthchecks/`wait-for` no Dockerfile para evitar race conditions na inicialização.
- Adicionar CI (GitHub Actions) para rodar `mvn test` e publicar relatório JaCoCo.
- Documentar a API com OpenAPI/Swagger.

Se quiser, eu:
- adiciono um README mais detalhado com exemplos adicionais de joltSpec;
- crio o workflow de CI que executa testes e gera relatório de cobertura;
- ou abro um PR com todas as mudanças aplicadas até aqui.

Contato
-------
Se surgir qualquer erro ao rodar os comandos acima cole aqui o output dos comandos (`docker compose ps`, `docker compose logs -f app`, `curl -v ...`) e eu te ajudo a resolver imediatamente.

