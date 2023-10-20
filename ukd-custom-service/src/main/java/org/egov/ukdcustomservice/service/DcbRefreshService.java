package org.egov.ukdcustomservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DcbRefreshService {

	@Autowired
	private JdbcTemplate jdbcTemplate; 
	
	public static final String tenantsQuery="select distinct tenantid from eg_pt_property where tenantid != 'uk.dehradun'";
	public static final String deleteQuery="delete from  dcb where propertyid in "+
	" ("
	+ "select propertyid from eg_pt_property prop,egbs_demand_v1 demand, egbs_demanddetail_v1 dd " +
	" where demand.id=dd.demandid and demand.consumercode=prop.propertyid and prop.tenantid=':tenantId' and "+
    " ( "
    + "to_timestamp(dd.lastmodifiedtime/1000)>(select case when max(updatedtime) is not null then max(updatedtime) else '2019-04-01' end from dcb where tenantid=':tenantId' ) "+
    " OR " + 
    " to_timestamp(prop.lastmodifiedtime/1000)>(select case when max(updatedtime) is not null then max(updatedtime) else '2019-04-01' end from dcb where tenantid=':tenantId' ) "
    + ") "
    + ")" +
    " and tenantid=':tenantId';";
	
	
	public boolean refresh(String tenant)
	{
 
		log.info("Refresh initiated  for "+tenant);
		String insertQuery="insert into  dcb  "+
				"SELECT"+
				"     prop.propertyid AS propertyid,"+
				"     prop.oldpropertyid AS oldpropertyid,"+
				"     address.doorno AS doorno,"+
				"     msg.message AS mohalla,"+
				"     prop.propertytype AS propertytype,"+
				"     prop.createdtime AS createddate,"+
				"     prop.usagecategory AS usage,"+
				"     prop.tenantid as tenantid,"+
				"     COALESCE(("+
				"     SELECT"+
				"     SUM(dd.taxamount) "+
				"     FROM"+
				"     egbs_demand_v1 demand, egbs_demanddetail_v1 dd "+
				"     WHERE"+
				"      demand.tenantid = dd.tenantid "+
				"     AND demand.id = dd.demandid "+
				"     AND dd.taxheadcode IN "+
				"     ("+
				"     'PT_TAX',"+
				"     'SWATCHATHA_TAX',"+
				"     'PT_ROUNDOFF'"+
				"     )"+
				"     AND "+
				"     ("+
				"     EXTRACT(epoch "+
				"     FROM"+
				"     now())*1000 BETWEEN demand.taxperiodfrom AND demand.taxperiodto"+
				"     )"+
				"     AND demand.consumercode = prop.propertyid), 0) currenttax,"+
				""+
				"COALESCE(("+
				"     SELECT"+
				"     SUM(unit.arv) "+
				"     FROM"+
				"      eg_pt_unit unit,eg_pt_asmt_assessment  ptd, eg_pt_asmt_unitusage auu"+
				"	where ptd.propertyid=prop.propertyid"+
				"   and auu.assessmentid=ptd.id"+
				"   and auu.unitid=unit.id"+
				"	and substr(ptd.financialyear, 0, 5)::INTEGER >=	to_char(current_timestamp, 'YYYY')::INTEGER ),0)"+
				"      currentarv,"+
				"COALESCE(("+
				"     SELECT"+
				"     SUM(unit.arv) "+
				"     FROM"+
				"      eg_pt_unit unit,eg_pt_asmt_assessment  ptd, eg_pt_asmt_unitusage auu"+
				"	where ptd.propertyid=prop.propertyid"+
				"   and auu.assessmentid=ptd.id"+
				"   and auu.unitid=unit.id"+
				"	and substr(ptd.financialyear, 0, 5)::INTEGER <	to_char(current_timestamp, 'YYYY')::INTEGER ),0)"+
				"      oldarv,"+
				"      "+
				"     COALESCE(("+
				"     SELECT"+
				"     SUM(dd.taxamount) "+
				"     FROM"+
				"     egbs_demand_v1 demand, egbs_demanddetail_v1 dd "+
				"     WHERE"+
				"      demand.tenantid = dd.tenantid "+
				"     AND demand.id = dd.demandid "+
				"     AND dd.taxheadcode IN "+
				"     ("+
				"     'PT_TAX',"+
				"     'SWATCHATHA_TAX',"+
				"     'PT_ROUNDOFF'"+
				"     )"+
				"     AND "+
				"     ("+
				"     EXTRACT(epoch "+
				"     FROM"+
				"     now())*1000 NOT BETWEEN demand.taxperiodfrom AND demand.taxperiodto"+
				"     )"+
				"     AND demand.consumercode = prop.propertyid), 0) arreartax,"+
				"     COALESCE(("+
				"     SELECT"+
				"     SUM(dd.taxamount) "+
				"     FROM"+
				"     egbs_demand_v1 demand, egbs_demanddetail_v1 dd "+
				"     WHERE"+
				"      demand.tenantid = dd.tenantid "+
				"     AND demand.id = dd.demandid "+
				"     AND dd.taxheadcode IN "+
				"     ("+
				"     'PT_TIME_INTEREST',"+
				"     'PT_TIME_PENALTY',"+
				"     'PT_LATE_ASSESSMENT_PENALTY'"+
				"     )"+
				"     AND demand.consumercode = prop.propertyid), 0) penaltytax,"+
				"     COALESCE(("+
				"     SELECT"+
				"     SUM(dd.taxamount) "+
				"     FROM"+
				"     egbs_demand_v1 demand, egbs_demanddetail_v1 dd "+
				"     WHERE"+
				"      demand.tenantid = dd.tenantid "+
				"     AND demand.id = dd.demandid "+
				"     AND dd.taxheadcode like '%REBATE%'"+
				""+
				"     AND demand.consumercode = prop.propertyid), 0) rebate,"+
				"     COALESCE(("+
				"     SELECT"+
				"     SUM(dd.taxamount) "+
				"     FROM"+
				"     egbs_demand_v1 demand, egbs_demanddetail_v1 dd "+
				"     WHERE"+
				"      demand.tenantid = dd.tenantid "+
				"     AND demand.id = dd.demandid "+
				"     AND demand.consumercode = prop.propertyid), 0) totaltax,"+
				"     COALESCE(("+
				"     SELECT"+
				"     SUM(dd.collectionamount) "+
				"     FROM"+
				"     egbs_demand_v1 demand, egbs_demanddetail_v1 dd "+
				"     WHERE"+
				"      demand.tenantid = dd.tenantid "+
				"     AND demand.id = dd.demandid "+
				"     AND dd.taxheadcode IN "+
				"     ("+
				"     'PT_TAX',"+
				"     'SWATCHATHA_TAX',"+
				"     'PT_ROUNDOFF'"+
				"     )"+
				"     AND "+
				"     ("+
				"     EXTRACT(epoch "+
				"     FROM"+
				"     now())*1000 BETWEEN demand.taxperiodfrom AND demand.taxperiodto"+
				"     )"+
				"     AND demand.consumercode = prop.propertyid), 0) currentcollected,"+
				"     COALESCE(("+
				"     SELECT"+
				"     SUM(dd.collectionamount) "+
				"     FROM"+
				"     egbs_demand_v1 demand, egbs_demanddetail_v1 dd "+
				"     WHERE"+
				"      demand.tenantid = dd.tenantid "+
				"     AND demand.id = dd.demandid "+
				"     AND dd.taxheadcode IN "+
				"     ("+
				"     'PT_TAX',"+
				"     'SWATCHATHA_TAX',"+
				"     'PT_ROUNDOFF'"+
				"     )"+
				"     AND "+
				"     ("+
				"     EXTRACT(epoch "+
				"     FROM"+
				"     now())*1000 NOT BETWEEN demand.taxperiodfrom AND demand.taxperiodto"+
				"     )"+
				"     AND demand.consumercode = prop.propertyid), 0) arrearcollected,"+
				"     COALESCE(("+
				"     SELECT"+
				"     SUM(dd.collectionamount) "+
				"     FROM"+
				"     egbs_demand_v1 demand, egbs_demanddetail_v1 dd "+
				"     WHERE"+
				"      demand.tenantid = dd.tenantid "+
				"     AND demand.id = dd.demandid "+
				"     AND dd.taxheadcode IN "+
				"     ("+
				"     'PT_TIME_INTEREST',"+
				"     'PT_TIME_PENALTY',"+
				"     'PT_LATE_ASSESSMENT_PENALTY'"+
				"     )"+
				"     AND demand.consumercode = prop.propertyid), 0) penaltycollected,"+
				"     COALESCE(("+
				"     SELECT"+
				"     SUM(dd.collectionamount) "+
				"     FROM"+
				"     egbs_demand_v1 demand, egbs_demanddetail_v1 dd "+
				"     WHERE"+
				"      demand.tenantid = dd.tenantid "+
				"     AND demand.id = dd.demandid "+
				"     AND dd.taxheadcode not like '%REBATE%'"+
				"     AND demand.consumercode = prop.propertyid), 0) totalcollected , "+
				"	    now() AS updatedtime," +
				"   (select  u.name from eg_pt_owner_v2 po, eg_user u   where "+
				"	 po.userid=u.uuid and "+
				"	 po.propertydetail = "+
				"     ("+
				"     SELECT"+
				"     assessmentnumber "+
				"     FROM"+
				"     eg_pt_propertydetail_v2 "+
				"     WHERE"+
				"     property = prop.propertyid "+
				"		 and "+
				"		 tenantid=prop.tenantid"+
				"     ORDER BY"+
				"     substr(financialyear, 0, 5)::INTEGER DESC LIMIT 1"+
				"     ) limit 1)  "+
				" AS name, "+
				"     COALESCE(("+
				"     SELECT"+
				"     SUM(dd.collectionamount) "+
				"     FROM"+
				"     egbs_demand_v1 demand, egbs_demanddetail_v1 dd "+
				"     WHERE"+
				"      demand.tenantid = dd.tenantid "+
				"     AND demand.id = dd.demandid "+
				"     AND dd.taxheadcode like '%REBATE%'"+
				"     AND "+
				"     ("+
				"     EXTRACT(epoch "+
				"     FROM"+
				"     now())*1000 BETWEEN demand.taxperiodfrom AND demand.taxperiodto"+
				"     )"+
				"     AND demand.consumercode = prop.propertyid), 0) currentrebate "+
				"     FROM "+
				"     eg_pt_property prop,"+
				"     eg_pt_asmt_assessment pd,"+
				"     eg_pt_address address,"+
				"     message msg "+
				"     WHERE"+
				"     assessmentnumber = "+
				"     ("+
				"     SELECT"+
				"     assessmentnumber "+
				"     FROM"+
				"     eg_pt_asmt_assessment "+
				"     WHERE"+
				"     propertyid = prop.propertyid "+
				"     ORDER BY"+
				"     substr(financialyear, 0, 5)::INTEGER DESC LIMIT 1"+
				"     )"+
				"     AND prop.propertyid = pd.propertyid "+
				"     AND prop.id = address.propertyid "+
				"     AND UPPER(replace(address.tenantid, '.', '_')) || '_REVENUE_' || address.locality = msg.code "+
				"     AND msg.locale = 'en_IN' "+
				"     AND prop.tenantid = pd.tenantid "+
				"     and msg.tenantid=prop.tenantid"+
				"     AND prop.tenantid = address.tenantid "+
				"     and prop.tenantid=':tenantId' "
				+ "  and prop.propertyid in  " +
				" ( "  +
				" select propertyid from eg_pt_property prop,egbs_demand_v1 demand, egbs_demanddetail_v1 dd " +
				" where demand.id=dd.demandid and demand.consumercode=prop.propertyid and prop.tenantid=':tenantId' and "+
			    " ( "
			    + "to_timestamp(dd.lastmodifiedtime/1000)>(select case when max(updatedtime) is not null then max(updatedtime) else '2019-04-01' end from dcb where tenantid=':tenantId' ) "+
			    " OR " + 
			    " to_timestamp(prop.lastmodifiedtime/1000)>(select case when max(updatedtime) is not null then max(updatedtime) else '2019-04-01' end from dcb where tenantid=':tenantId' ) "
			    + ") "
			    + ") "
				+ ";";
		
		String deletedQuery=	deleteQuery.replaceAll(":tenantId", tenant);
	  String	insertedQuery=	insertQuery.replaceAll(":tenantId", tenant);
		log.info("deleted existing for "+tenant +" deletedQuery : "+deletedQuery);

		jdbcTemplate.execute(deletedQuery);
		log.info("insert query for "+tenant +" insert : "+insertedQuery);
		
		jdbcTemplate.execute(insertedQuery );
		log.info("refresh completed for "+tenant);


		return true;
	}
	

	public List<String> getTenants() {
		 List<String> tenants;
		 tenants = jdbcTemplate.queryForList(tenantsQuery, String.class);
		 return tenants;
		 
		
	}
	
	


}
