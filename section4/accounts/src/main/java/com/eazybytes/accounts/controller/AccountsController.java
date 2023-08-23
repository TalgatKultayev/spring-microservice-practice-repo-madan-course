/**
 * 
 */
package com.eazybytes.accounts.controller;

import com.eazybytes.accounts.config.AccountsServiceConfig;
import com.eazybytes.accounts.feignclients.CardsFeignClient;
import com.eazybytes.accounts.feignclients.LoansFeignClient;
import com.eazybytes.accounts.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.eazybytes.accounts.repository.AccountsRepository;

import java.util.List;


/**
 * @author Eazy Bytes
 *
 */

@RestController
public class AccountsController {
	
	@Autowired
	private AccountsRepository accountsRepository;

	@Autowired
	private AccountsServiceConfig accountsServiceConfig;

	@Autowired
	private CardsFeignClient cardsFeignClient;

	@Autowired
	private LoansFeignClient loansFeignClient;

	@PostMapping("/myAccount")
	public Accounts getAccountDetails(@RequestBody Customer customer) {

		Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
		if (accounts != null) {
			return accounts;
		} else {
			return null;
		}

	}

	@GetMapping("/accounts/properties")
	public String getPropertyDetails() throws JsonProcessingException {
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

		Properties properties = new Properties(accountsServiceConfig.getMsg(), accountsServiceConfig.getBuildVersion(),
				accountsServiceConfig.getMailDetails(), accountsServiceConfig.getActiveBranches());

        return ow.writeValueAsString(properties);
	}

	@PostMapping("/myCustomerDetails")
	@CircuitBreaker(name = "detailsForCustomerSupportApp", fallbackMethod = "myCustomerDetailsFallBack")
	@Retry(name = "retryForCustomerDetails", fallbackMethod = "myCustomerDetailsFallBack")
	@RateLimiter(name="detailsForCustomerSupportApp", fallbackMethod = "myCustomerDetailsFallBack")
	@Bulkhead(name="bulkheadAccounts", fallbackMethod = "bulkheadAccountsFallBack")
	public CustomerDetails myCustomerDetails(@RequestBody Customer customer) {
		Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
		List<Loans> loans = loansFeignClient.getLoanDetails(customer);
		List<Cards> cards = cardsFeignClient.getCardDetails(customer);

		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setAccounts(accounts);
		customerDetails.setLoans(loans);
		customerDetails.setCards(cards);

		return customerDetails;
	}

	private CustomerDetails myCustomerDetailsFallBack(Customer customer, Throwable throwable) {
		Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
		List<Loans> loans = loansFeignClient.getLoanDetails(customer);

		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setAccounts(accounts);
		customerDetails.setLoans(loans);

		return customerDetails;
	}

}
