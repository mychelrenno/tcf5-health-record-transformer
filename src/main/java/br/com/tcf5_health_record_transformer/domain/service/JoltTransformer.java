package br.com.tcf5_health_record_transformer.domain.service;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class JoltTransformer {

    public String transform(String inputJson, String specJson) {
        Object input = JsonUtils.jsonToObject(inputJson);
        List<Object> spec = JsonUtils.jsonToList(specJson);
        Chainr chainr = Chainr.fromSpec(spec);
        Object transformed = chainr.transform(input);
        return JsonUtils.toJsonString(transformed);
    }
}