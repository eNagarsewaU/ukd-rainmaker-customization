package org.egov.ukdcustomservice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.egov.ukdcustomservice.web.models.DemandAutoGenerate;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AutomaticDemandGenerationService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public static final String tenantsQuery = "select distinct tenantid from eg_pt_property where tenantid != 'uk.dehradun'";
 
	public boolean AutomaticDemandGeneration() {
		String result = "";
		String tenant = "uk.testing";
		String fyear = "2024-2025";

		ZoneId zoneId = ZoneId.of("Etc/UTC");
		LocalDate today = LocalDate.now(zoneId);

		LocalDateTime fromTime = LocalDateTime.parse(today + "T00:00:00");
		long taxPeriodFrom = fromTime.atZone(zoneId).toInstant().toEpochMilli();

		LocalDateTime toTime = LocalDateTime.parse(today + "T23:59:59");
		long taxPeriodTo = toTime.atZone(zoneId).toInstant().toEpochMilli();

		log.info("Automatic Demand Generation initiated  for " + tenant);
		String Query = "select eg_pt_property.tenantid, eg_pt_owner.userid, eg_pt_property.propertyid, eg_pt_property.propertyType, sum(eg_pt_unit.arv) FROM eg_pt_property FULL JOIN eg_pt_owner ON eg_pt_owner.propertyid = eg_pt_property.id FULL JOIN  eg_pt_unit ON eg_pt_unit.propertyid = eg_pt_property.id where eg_pt_unit.propertyid in (select id from eg_pt_property where createdtime between "
				+ "'" + taxPeriodFrom + "'" + " AND " + "'" + taxPeriodTo + "'" + " and tenantid=" + "'" + tenant + "'"
				+ ") GROUP BY eg_pt_owner.userid, eg_pt_property.propertyid, eg_pt_property.propertyType, eg_pt_property.tenantid;";

		log.info("Query Run for Automatic Demand for " + tenant + " FindQuery : " + Query);

		jdbcTemplate.execute(Query);
		List<DemandAutoGenerate> data;
		data = jdbcTemplate.queryForList(Query, DemandAutoGenerate.class);
		log.info("Data List for " + tenant + " insert : " + data.toString());

		int j = data.size();

		ResponseEntity<String> response = null;
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String token = getToken(tenant);

		if (token.equals("NA")) {
			log.info("Token Failer for" + tenant);
			log.info("Token is: " + token);
			return false;
		} else {
			log.info("Token for" + tenant + " is : " + token);
		}

		for (int i = 0; i < j; i++) {
			String tenantId = data.get(i).getTenantId();
			String uuid = data.get(i).getUserId();
			String ptId = data.get(i).getPropertyId();
			String consumerType = data.get(i).getPropertyType();
			String Arv = data.get(i).getSum();
			int arv = Integer.parseInt(Arv);
			int amount = (arv * 10) / 100;

			JSONObject searchRequestInfo = new JSONObject();

			JSONObject assessmentInfo = new JSONObject();

			JSONObject additionalDetails = new JSONObject();

			JSONObject RequestInfo2 = new JSONObject();

			JSONObject DemandObj = new JSONObject();
			JSONObject DemandObj2 = new JSONObject();

			JSONObject PayerObj = new JSONObject();
			JSONObject PayerObj2 = new JSONObject();

			JSONObject DemandDetailsObj = new JSONObject();
			JSONObject DemandDetailsObjs = new JSONObject();
			JSONObject DemandDetailsObjs2 = new JSONObject();
			JSONObject DemandDetailsObj2 = new JSONObject();
			JSONObject DemandDetailsObj3 = new JSONObject();

			// JSONObject DemandHead = new JSONObject();

			JSONObject Main = new JSONObject();
			JSONArray Demand = new JSONArray();
			JSONArray Demand2 = new JSONArray();
			JSONArray Demand3 = new JSONArray();
			JSONArray DemandDetails = new JSONArray();

			searchRequestInfo.put("api_id", "Rainmaker");
			searchRequestInfo.put("ver", ".01");
			searchRequestInfo.put("ts", "");
			searchRequestInfo.put("action", "_create");
			searchRequestInfo.put("did", "1");
			searchRequestInfo.put("key", "");
			searchRequestInfo.put("msgId", "20170310130900|en_IN");
			searchRequestInfo.put("authToken", token);

			assessmentInfo.put("tenantId", tenant);
			assessmentInfo.put("propertyId", ptId);
			assessmentInfo.put("financialYear", fyear);
			assessmentInfo.put("assessmentDate", "1714852041000");
			assessmentInfo.put("source", "LEGACY_RECORD");
			assessmentInfo.put("channel", "CFC_COUNTER");
			assessmentInfo.put("status", "ACTIVE");

			RequestInfo2.put("tt", "tt");

			additionalDetails.put("RequestInfo", RequestInfo2);
			additionalDetails.put("reassement", "false");

			DemandObj.put("tenantId", tenant);
			DemandObj.put("consumerCode", ptId);
			DemandObj.put("consumerType", consumerType);
			DemandObj.put("businessService", "PT");
			DemandObj.put("taxPeriodFrom", "1711929600000");
			DemandObj.put("taxPeriodTo", "1743465599000");

			DemandDetailsObj.put("taxHeadMasterCode", "PT_TAX");
			DemandDetailsObj.put("taxAmount", amount);
			DemandDetailsObj.put("collectionAmount", 0);

//			For Haldwani

//			DemandDetailsObjs.put("taxHeadMasterCode", "SWATCHATHA_TAX");
//			DemandDetailsObjs.put("taxAmount", amount);
//			DemandDetailsObjs.put("collectionAmount", 0);

			Demand2.add(DemandDetailsObj);

//			For Haldwani

//			Demand2.add(DemandDetailsObjs);

			PayerObj.put("uuid", uuid);

			DemandObj.put("payer", PayerObj);
			DemandObj.put("demandDetails", Demand2);

			DemandObj2.put("tenantId", tenant);
			DemandObj2.put("consumerCode", ptId);
			DemandObj2.put("consumerType", consumerType);
			DemandObj2.put("businessService", "PT");
			DemandObj2.put("taxPeriodFrom", "1680307200000");
			DemandObj2.put("taxPeriodTo", "1711929599000");

			DemandDetailsObj3.put("taxHeadMasterCode", "PT_TAX");
			DemandDetailsObj3.put("taxAmount", amount);
			DemandDetailsObj3.put("collectionAmount", 0);

//			For Haldwani

//			DemandDetailsObjs2.put("taxHeadMasterCode", "SWATCHATHA_TAX");
//			DemandDetailsObjs2.put("taxAmount", amount);
//			DemandDetailsObjs2.put("collectionAmount", 0);

			Demand3.add(DemandDetailsObj3);
//			For Haldwani
//			Demand3.add(DemandDetailsObjs2);

			PayerObj2.put("uuid", uuid);

			DemandObj2.put("payer", PayerObj2);
			DemandObj2.put("demandDetails", Demand3);

			DemandDetails.add(DemandDetailsObj2);
			Demand.add(DemandObj);
			Demand.add(DemandObj2);

			additionalDetails.put("Demands", Demand);

			assessmentInfo.put("additionalDetails", additionalDetails);

			assessmentInfo.put("additionalDetails", additionalDetails);

			Main.put("RequestInfo", searchRequestInfo);
			Main.put("Assessment", assessmentInfo);

			System.out.println(Main.toString());
			// System.out.println("Demand Initiated for PropertId: "+ptId);
			String fetchUserDetailsUrl = "https://nagarsewa.uk.gov.in/property-services/assessment/_create?tenantId="
					+ tenant;
			try {
				HttpEntity<String> searchRequesthead = new HttpEntity<String>(Main.toString(), headers);
				response = restTemplate.postForEntity(fetchUserDetailsUrl, searchRequesthead, String.class);

				String statusCode = response.getStatusCode().toString();
				String result2 = ptId + ":" + statusCode + "\n";
				result = result + result2;
				System.out.println("========================Property Count for Pass: ======================" + i);
				log.info("Exception for PropertyId: " + ptId);
				log.info("Result: " + result2);

			} catch (Exception e) {

				System.out.println("PropertyId: " + ptId + " .Showing error: error");
				// String result2 = ptId + ":" + error + "\n";
				String error = "error";
				System.out.println(e);
				String result2 = ptId + ":" + error + "\n";
				result = result + result2;
				System.out.println("========================Property Count for Fail: ======================" + i);
				log.info("Exception for PropertyId: " + ptId);
				log.info("Result during Exception: " + e);
				continue;
			}

		}

		return true;
	}

	public String getToken(String tenant) {
		RestTemplate restTemplate = new RestTemplate();
		String authToken = "";
		String url = "https://nagarsewa.uk.gov.in/user/oauth/token";
		HttpHeaders header = new HttpHeaders();
		header.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		header.set("authorization", "Basic ZWdvdi11c2VyLWNsaWVudDo=");

		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("username", "CounterEmployee-testing");
		map.add("password", "eGov@123");
		map.add("grant_type", "password");
		map.add("scope", "read");
		map.add("tenantId", "uk.testing");
		map.add("userType", "EMPLOYEE");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, header);
		ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

		String status = response.getStatusCode().toString();
		if (status.equals("200 OK")) {
			String body = response.getBody();
			String a[] = body.split("\",\"token_type\"");
			String b = a[0];
			String token[] = b.split(":\"");
			authToken = token[1];
			System.out.println(authToken);
		} else {
			authToken = "NA";
		}

		return authToken;
	}

	public List<String> getTenants() {
		List<String> tenants;
		tenants = jdbcTemplate.queryForList(tenantsQuery, String.class);
		return tenants;

	}

}
