package com.aerospike.examples;

import java.util.ArrayList;
import java.util.HashMap;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Language;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;

public class UserDefinedFunction extends Example {

	public UserDefinedFunction(Console console) {
		super(console);
	}

	/**
	 * Register user defined function and call it.
	 */
	@Override
	public void runExample(AerospikeClient client, Parameters params) throws Exception {
		if (! params.hasUdf) {
			console.info("User defined functions are not supported by the connected Aerospike server.");
			return;
		}
		register(client);
		example1(client, params);
		example2(client, params);
	}
	
	private void register(AerospikeClient client) throws Exception {
		Policy policy = new Policy();
		policy.timeout = 5000; // Registration takes longer than a normal request.
		client.register(policy, "udf/records.lua", "records.lua", Language.LUA);
	}
	

	private void example1(AerospikeClient client, Parameters params) throws Exception {	
		Key key = new Key(params.namespace, params.set, "udfkey");
		Bin bin = new Bin(params.getBinName("udfbin"), "string value");
		
		// Delete record if it already exists.
		client.delete(params.writePolicy, key);

		client.put(params.writePolicy, key, bin);
		
		String expected = bin.value.toString();
		String received = (String)client.execute(params.policy, key, "records", "getbin", Value.get(bin.name));

		if (received != null && received.equals(expected)) {
			console.info("Bin matched: namespace=%s set=%s key=%s bin=%s value=%s", 
				key.namespace, key.setName, key.userKey, bin.name, received);
		}
		else {
			console.error("UDF mismatch: Expected %s. Received %s.", expected, received);
		}
	}
	
	private void example2(AerospikeClient client, Parameters params) throws Exception {	
		Key key = new Key(params.namespace, params.set, "udfcomplexkey");
		
		// Delete record if it already exists.
		client.delete(params.writePolicy, key);

		ArrayList<Object> inner = new ArrayList<Object>();
		inner.add("string2");
		inner.add(8L);
		
		HashMap<Object,Object> innerMap = new HashMap<Object,Object>();
		innerMap.put("a", 1L);
		innerMap.put(2L, "b");
		innerMap.put("list", inner);
		
		ArrayList<Object> list = new ArrayList<Object>();
		list.add("string1");
		list.add(4L);
		list.add(inner);
		list.add(innerMap);

		Bin bin = new Bin(params.getBinName("udfcomplexbin"), Value.getAsList(list));

		client.put(params.writePolicy, key, bin);
		
		Object received = client.execute(params.policy, key, "records", "getbin", Value.get(bin.name));

		if (received != null && received.equals(list)) {
			console.info("UDF data matched: namespace=%s set=%s key=%s bin=%s value=%s", 
				key.namespace, key.setName, key.userKey, bin.name, received);
		}
		else {
			console.error("UDF data mismatch");
			console.error("Expected " + list);
			console.error("Received " + received);
		}
	}	
}
