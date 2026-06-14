package com.mockify.backend.dto.response.imports;

import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiImportResponse {

    private List<MockSchemaResponse> imported;
    private List<SkippedSchema> skipped;
    private int totalImported;
    private int totalSkipped;

    public static OpenApiImportResponse of(List<MockSchemaResponse> imported, List<SkippedSchema> skipped) {
        OpenApiImportResponse res = new OpenApiImportResponse();

        res.imported = imported == null ? Collections.emptyList() : imported;
        res.skipped = skipped == null ? Collections.emptyList() : skipped;

        res.totalImported = res.imported.size();
        res.totalSkipped = res.skipped.size();
        return res;
    }
}
