package org.egov.ukdcustomservice.web.controller;

import java.util.List;

import org.egov.ukdcustomservice.service.DcbRefreshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class DcbRefreshController {

    @Autowired
    private DcbRefreshService dcbRefreshService;

    @RequestMapping(value = "/v1/pt/dcb/_refresh", method = RequestMethod.GET )
    public ResponseEntity<Object> method() {

        HttpHeaders httpHeaders = new HttpHeaders();
        List<String> tenants = dcbRefreshService.getTenants();
        for(String tenant:tenants)
        {
        dcbRefreshService.refresh(tenant);
        }
        return new ResponseEntity<>(httpHeaders, HttpStatus.FOUND);  
    }

    //this method help to refresh specific tenant only
    @RequestMapping(value = "/v1/pt/dcb/_refreshSpecificTenant", method = RequestMethod.GET)
	public ResponseEntity<Object> updateDcbForSpecificTenant(@RequestParam("tenant") String tenant) {

		HttpHeaders httpHeaders = new HttpHeaders();
		dcbRefreshService.refresh(tenant);
		return new ResponseEntity<>(httpHeaders, HttpStatus.FOUND);
	}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleError(Exception e) {
        log.error("EXCEPTION_WHILE_REFRESHING", e);
        HttpHeaders httpHeaders = new HttpHeaders();
       // httpHeaders.setLocation(UriComponentsBuilder.fromHttpUrl(defaultURL).build().encode().toUri());
        return new ResponseEntity<>(httpHeaders, HttpStatus.FOUND);
    }

}
