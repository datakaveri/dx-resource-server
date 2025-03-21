package iudx.resource.server.apiserver.subscription.model;

import java.util.List;

public record GetSubscriptionResult(List<String> listString, String entities) {
}
