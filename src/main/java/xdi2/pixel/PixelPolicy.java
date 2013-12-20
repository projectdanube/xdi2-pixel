package xdi2.pixel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xdi2.core.Graph;
import xdi2.core.features.linkcontracts.LinkContractBase;
import xdi2.core.features.linkcontracts.LinkContracts;
import xdi2.core.features.linkcontracts.condition.GenericCondition;
import xdi2.core.features.linkcontracts.condition.IsCondition;
import xdi2.core.features.linkcontracts.operator.FalseOperator;
import xdi2.core.features.linkcontracts.operator.TrueOperator;
import xdi2.core.features.linkcontracts.policy.Policy;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.xri3.XDI3Segment;
import xdi2.core.xri3.XDI3Statement;

public class PixelPolicy {

	private HashMap<?, ?> hashMap;
	private LinkContractBase linkContract;

	private PixelPolicy(HashMap<?, ?> hashMap, LinkContractBase linkContract) { 

		this.hashMap = hashMap;
		this.linkContract = linkContract;
	}

	static PixelPolicy fromMap(HashMap<?, ?> hashMap) throws PixelParserException {

		LinkContractBase linkContract = translate(hashMap);

		PixelPolicy pixelPolicy = new PixelPolicy(hashMap, linkContract);

		return pixelPolicy;
	}

	private static LinkContractBase translate(HashMap<?, ?> hashMap) throws PixelParserException {

		Graph graph = MemoryGraphFactory.getInstance().openGraph();
		LinkContractBase linkContract = LinkContracts.getLinkContract(graph.getRootContextNode(), true);

		linkContract.setPermissionTargetAddress(XDI3Segment.create("$do$signal"), XDI3Segment.create("[+channel]{}[+event]{}"));

		Policy xdiPolicyRoot = linkContract.getPolicyRoot(true);

		// 'decls'

		List<?> decls = (List<?>) hashMap.get("decls");

		Map<String, String> declsMap = new HashMap<String, String> ();

		if (decls != null) {

			for (Object decl : decls) {

				String expr = (String) ((Map<?, ?>) decl).get("expr");
				String lhs = (String) ((Map<?, ?>) decl).get("lhs");

				declsMap.put(lhs, expr);
			}
		}

		// 'policy'

		List<?> policies = (List<?>) hashMap.get("policy");
		if (policies == null) return linkContract;

		int countDeny = countDenyEffect(policies);
		int countAllow = countAllowEffect(policies);

		for (Object policy : policies) {

			// translate effect of current policy statement

			String effect = (String) ((Map<?, ?>) policy).get("effect");

			Policy xdiPolicy = xdiPolicyRoot;

			if (isAllow(effect)) {

				if (countDeny > 0) {

					xdiPolicy = xdiPolicy.createAndPolicy(true);
				}

				if (countAllow > 1) {

					xdiPolicy = xdiPolicy.createOrPolicy(true);
				}

				xdiPolicy = xdiPolicy.createAndPolicy(countAllow == 1);
			} else if (isDeny(effect)) {

				if (countAllow > 0) {

					xdiPolicy = xdiPolicy.createAndPolicy(true);
				}

				xdiPolicy = xdiPolicy.createNotPolicy(true);

				if (countDeny > 1) {

					xdiPolicy = xdiPolicy.createOrPolicy(true);
				}

				xdiPolicy = xdiPolicy.createAndPolicy(countDeny == 1);
			}

			// translate details of current policy statement

			translatePolicyStmt((Map<?, ?>) policy, declsMap, xdiPolicy);
		}

		// done

		return linkContract;
	}

	private static int countDenyEffect(List<?> policies) {

		int count = 0;

		for (Object policy : policies) {

			if (isDeny((String) ((Map<?, ?>) policy).get("effect"))) count++;
		}

		return count;
	}

	private static int countAllowEffect(List<?> policies) {

		int count = 0;

		for (Object policy : policies) {

			if (isAllow((String) ((Map<?, ?>) policy).get("effect"))) count++;
		}

		return count;
	}

	private static boolean isDeny(String effect) {

		return "deny".equals(effect) || "denies".equals(effect);
	}

	private static boolean isAllow(String effect) {

		return "allow".equals(effect) || "allows".equals(effect);
	}

	private static void translatePolicyStmt(Map<?, ?> policy, Map<String, String> declsMap, Policy xdiPolicy) throws PixelParserException {

		if (xdiPolicy == null) throw new NullPointerException();

		// 'channel_id'

		String channel_id = (String) policy.get("channel_id");

		String channelXriString;

		if ("any".equals(channel_id)) {

			channelXriString = "[+channel]{}";
		} else if (channel_id != null) {

			channelXriString = "[+channel]" + channel_id;
		} else {

			throw new PixelParserException("No 'channel_id'.");
		}

		// 'cloud_id'

		String cloud_id = (String) policy.get("cloud_id");

		if (cloud_id != null) {

			if (declsMap.containsKey(cloud_id)) cloud_id = declsMap.get(cloud_id);

			channelXriString = "(" + cloud_id + ")" + channelXriString;
		}

		// 'event_filter', 'domain', 'type'

		Map<?, ?> event_filter = (Map<?, ?>) policy.get("event_filter");

		String event_filter_domain = null;
		List<?> event_filter_types = null;

		if (event_filter != null) {

			event_filter_domain = (String) event_filter.get("domain");
			event_filter_types = (List<?>) event_filter.get("types");
		}

		// 'condition'

		Boolean condition_sense = null;
		List<?> condition_type_clouds = null;
		List<?> condition_type_relationships = null;

		Map<?, ?> condition = (Map<?, ?>) policy.get("condition");

		if (condition != null) {

			condition_sense = (Boolean) condition.get("sense");
			String condition_type = (String) condition.get("type");

			if ("relationship_list".equals(condition_type)) {

				condition_type_relationships = (List<?>) condition.get("relationship_list");
			} else if ("relationship_single".equals(condition_type)) {

				condition_type_relationships = Collections.singletonList(condition.get("relationship_id"));
			} else if ("raised_by_list".equals(condition_type)) {

				condition_type_clouds = (List<?>) condition.get("cloud_list");
			} else if ("raised_by_single".equals(condition_type)) {

				condition_type_clouds = Collections.singletonList(condition.get("cloud_id"));
			} else {

				throw new PixelParserException("Invalid 'condition' type: " + condition_type);
			}
		}

		// construct policy

		String eventXriString = channelXriString + "[+event]{1}";

		XDI3Segment eventXri = XDI3Segment.create(eventXriString);
		XDI3Segment operationXri = XDI3Segment.create("$do$signal");

		if (event_filter_domain != null) {

			if ("all".equals(event_filter_domain))
				event_filter_domain = "{}";
			else
				event_filter_domain = "+" + event_filter_domain;

			XDI3Statement statementXri = XDI3Statement.create("{$msg}$do/" + operationXri + "(" + eventXri + "/+domain/" + event_filter_domain + ")");

			TrueOperator.createTrueOperator(xdiPolicy, GenericCondition.fromStatement(statementXri));
		}

		if (event_filter_types != null && event_filter_types.size() > 0) {

			Policy policyEventFilterType = event_filter_types.size() > 1 ? xdiPolicy.createOrPolicy(false) : xdiPolicy;

			for (Object event_filter_type : event_filter_types) {

				XDI3Statement statementXri = XDI3Statement.create("{$msg}$do/" + operationXri + "(" + eventXri + "/+type/" + "+" + (String) event_filter_type + ")");

				TrueOperator.createTrueOperator(policyEventFilterType, GenericCondition.fromStatement(statementXri));
			}
		}

		if (condition_type_relationships != null) {

			Policy policyConditionTypeRelationships = condition_type_relationships.size() > 1 ? xdiPolicy.createOrPolicy(false) : xdiPolicy;

			for (Object condition_type_relationship : condition_type_relationships) {

				XDI3Statement statementXri = XDI3Statement.create("" + channelXriString + "/" + (String) condition_type_relationship + "/" + "{$from}");

				if (Boolean.TRUE.equals(condition_sense))
					TrueOperator.createTrueOperator(policyConditionTypeRelationships, GenericCondition.fromStatement(statementXri));
				else
					FalseOperator.createFalseOperator(policyConditionTypeRelationships, GenericCondition.fromStatement(statementXri));
			}
		}

		if (condition_type_clouds != null) {

			Policy policyConditionTypeClouds = condition_type_clouds.size() > 1 ? xdiPolicy.createOrPolicy(false) : xdiPolicy;

			for (Object condition_type_cloud : condition_type_clouds) {

				if (declsMap.containsKey(condition_type_cloud)) condition_type_cloud = declsMap.get(condition_type_cloud);

				XDI3Statement statementXri = XDI3Statement.create("" + (String) condition_type_cloud + "/" + "$is" + "/" + "{$from}");

				if (Boolean.TRUE.equals(condition_sense))
					TrueOperator.createTrueOperator(policyConditionTypeClouds, IsCondition.fromStatement(statementXri));
				else
					FalseOperator.createFalseOperator(policyConditionTypeClouds, IsCondition.fromStatement(statementXri));
			}
		}
	}

	public HashMap<?, ?> getHashMap() {

		return this.hashMap;
	}

	public LinkContractBase getLinkContract() {

		return this.linkContract;
	}
}
