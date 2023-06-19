package org.egov.ukdcustomservice.web.models;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.common.contract.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Contract class to send response. Array of Property items  are used in case of search results or response for create. Where as single Property item is used for update
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PropertyResponse   {
        @JsonProperty("ResponseInfo")
        private ResponseInfo responseInfo;

        @JsonProperty("Properties")
        @Valid
        private List<Property> properties;


        public PropertyResponse addPropertiesItem(Property propertiesItem) {
            if (this.properties == null) {
            this.properties = new ArrayList<>();
            }
        this.properties.add(propertiesItem);
        return this;
        }

}
