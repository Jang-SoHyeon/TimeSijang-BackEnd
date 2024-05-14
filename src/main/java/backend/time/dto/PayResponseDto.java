package backend.time.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PayResponseDto {
    Long amount;
    String status;
    String imp_uid;
    Long member_id;
}
