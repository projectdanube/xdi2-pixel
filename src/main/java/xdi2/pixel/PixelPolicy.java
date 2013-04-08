package xdi2.pixel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xdi2.core.Graph;
import xdi2.core.features.linkcontracts.LinkContract;
import xdi2.core.features.linkcontracts.LinkContracts;
import xdi2.core.features.linkcontracts.policy.Policy;
import xdi2.core.features.linkcontracts.policy.PolicyAnd;
import xdi2.core.features.linkcontracts.policy.PolicyRoot;
import xdi2.core.features.linkcontracts.policy.PolicyUtil;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.xri3.XDI3Segment;

public class PixelPolicy {

	private HashMap<?, ?> hashMap;
	private LinkContract linkContract;

	private PixelPolicy(HashMap<?, ?> hashMap, LinkContract linkContract) { 

		this.hashMap = hashMap;
		this.linkContract = linkContract;
	}

	static PixelPolicy fromMap(HashMap<?, ?> hashMap) throws PixelParserException {

		LinkContract linkContract = translate(hashMap);
		
		PixelPolicy pixelPolicy = new PixelPolicy(hashMap, linkContract);

		return pixelPolicy;
	}

	private static LinkContract translate(HashMap<?, ?> hashMap) throws PixelParserException {

		Graph graph = MemoryGraphFactory.getInstance().openGraph();
		LinkContract linkContract = LinkContracts.getLinkContract(graph.getRootContextNode(), true);

		PolicyRoot policyRoot = linkContract.getPolicyRoot(true);
		PolicyAnd policyAnd = policyRoot.createAndPolicy();

		List<?> policy_stmts = (List<?>) hashMap.get("policy_stmts");
		if (policy_stmts == null) return linkContract;

		for (int i=0; i<policy_stmts.size(); i++) {

			Map<?, ?> policy_stmt = (Map<?, ?>) policy_stmts.get(i);
			
			translatePolicyStmt(policy_stmt, policyAnd);
		}
		
		return linkContract;
	}

	private static void translatePolicyStmt(Map<?, ?> policy_stmt, PolicyAnd policyAnd) throws PixelParserException {

		String effect = (String) policy_stmt.get("effect");
		String cloud_id = (String) policy_stmt.get("cloud_id");
		String channel_id = (String) policy_stmt.get("channel_id");

		Policy policy;
		
		if ("allow".equals(effect)) {
			
			policy = policyAnd;
		} else if ("deny".equals(effect)) {
			
			policy = policyAnd.createNotPolicy();
		} else {
			
			throw new PixelParserException("Invalid 'effect'.");
		}
		
		if (cloud_id != null) PolicyUtil.createSenderMatchesOperator(policy, XDI3Segment.create(cloud_id));
		if (channel_id != null) PolicyUtil.createSenderMatchesOperator(policy, XDI3Segment.create(channel_id));
	}

	public HashMap<?, ?> getHashMap() {

		return this.hashMap;
	}
	
	public LinkContract getLinkContract() {
		
		return this.linkContract;
	}
}
