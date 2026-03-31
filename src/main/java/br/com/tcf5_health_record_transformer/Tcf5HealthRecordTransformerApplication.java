package br.com.tcf5_health_record_transformer;

import br.com.tcf5_health_record_transformer.domain.model.MappingRule;
import br.com.tcf5_health_record_transformer.infrastructure.repository.MappingRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

@SpringBootApplication
public class Tcf5HealthRecordTransformerApplication {

	public static void main(String[] args) {
		SpringApplication.run(Tcf5HealthRecordTransformerApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(MappingRuleRepository repository, JdbcTemplate jdbcTemplate) {
		return args -> {
			// Garante que a tabela health_records exista (cria apenas se não existir)
			String ddlHealth = "CREATE TABLE IF NOT EXISTS health_records (id bigserial PRIMARY KEY, patient_id text, resource_type text, client_id uuid, resource_content jsonb, processed_at timestamp);";
			jdbcTemplate.execute(ddlHealth);

			// Garante que a tabela mapping_rules exista (cria apenas se não existir)
			String ddl = "CREATE TABLE IF NOT EXISTS mapping_rules (client_id uuid PRIMARY KEY, jolt_spec text);";
			jdbcTemplate.execute(ddl);

			if (repository.count() == 0) {
				String defaultSpec = """
                    [
                      {
                        "operation": "shift",
                        "spec": {
                          "atendimento": {
                            "diagnostico_cid": "Condition.code.coding[0].code",
                            "condicao": "Condition.code.coding[0].display",
                            "data": "Encounter.period.start",
                            "pressao_arterial": "Observation.valueString"
                          },
                          "paciente": {
                            "cpf": "Patient.identifier[0].value",
                            "nome": "Patient.name[0].text"
                          }
                        }
                      }
                    ]
                    """;

				try {
					MappingRule mr = new MappingRule();
					UUID id = UUID.nameUUIDFromBytes("UPA_RECIFE_01".getBytes());
					mr.setClientId(id);
					mr.setJoltSpec(defaultSpec);
					repository.save(mr);
					System.out.println(">>> Banco inicializado com a regra JOLT da UPA_RECIFE_01 (UUID: " + id + ")");
				} catch (Exception e) {
					System.err.println(">>> Falha ao inserir regra inicial (ignorando): " + e.getMessage());
					// opcional: e.printStackTrace();
				}
			} else {
				System.out.println(">>> Banco já possui regras. Pulando inicialização.");
			}
		};
	}

}
