package org.egov.ukdcustomservice.web.models;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandAutoGenerate {

	@NotNull
	private String tenantId;
	private String userId;
	private String propertyId;
	private String propertyType;
	private String sum;
	
}
