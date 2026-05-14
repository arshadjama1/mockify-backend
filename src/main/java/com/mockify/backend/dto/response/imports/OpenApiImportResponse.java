package com.mockify.backend.dto.response.imports;

import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import lombok.*;
import java.util.List;

public class OpenApiImportResponse {

    private List<MockSchemaResponse> imported;
    private List<SkippedSchema> skipped;
    private int totalImported;
    private int totalSkipped;

    public static OpenApiImportResponse of(List<MockSchemaResponse> imported, List<SkippedSchema> skipped) {
        OpenApiImportResponse res = new OpenApiImportResponse();
        res.imported = imported;
        res.skipped = skipped;
        res.totalImported = imported.size();
        res.totalSkipped = skipped.size();
        return res;
    }
}
