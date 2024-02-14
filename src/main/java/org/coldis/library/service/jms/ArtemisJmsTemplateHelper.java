package org.coldis.library.service.jms;

import java.util.Map;
import java.util.Random;

import org.apache.activemq.artemis.api.core.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * JMS template helper.
 */
@Component
@ConditionalOnClass(value = { JmsTemplate.class, ArtemisProperties.class })
public class ArtemisJmsTemplateHelper implements JmsTemplateHelper {

	/**
	 * Random.
	 */
	private static final Random RANDOM = new Random();

	/**
	 * @see org.coldis.library.service.jms.JmsTemplateHelper#send(org.springframework.jms.core.JmsTemplate,
	 *      org.coldis.library.service.jms.JmsMessage)
	 */
	@Override
	public void send(
			final JmsTemplate template,
			final JmsMessage<?> message) {
		// Tries sending the message.
		template.send(message.getDestination(), session -> {
			// Creates the message.
			final jakarta.jms.Message jmsMessage = template.getMessageConverter().toMessage(message.getMessage(), session);
			if ((message.getFixedDelay().isPositive()) || (message.getRandomDelay().isPositive())) {
				final long scheduledTimestamp = System.currentTimeMillis()
						+ ((message.getFixedDelay().toMillis() + (Math.abs(ArtemisJmsTemplateHelper.RANDOM.nextLong(message.getRandomDelay().toMillis())))));
				jmsMessage.setLongProperty(Message.HDR_SCHEDULED_DELIVERY_TIME.toString(), scheduledTimestamp);
			}
			// Sets the priority.
			if (message.getPriority() != null) {
				jmsMessage.setJMSPriority(Math.max(1, Math.min(message.getPriority().intValue(), 9)));
			}
			// Sets the correlation id.
			if (message.getCorrelationId() != null) {
				jmsMessage.setJMSCorrelationID(message.getCorrelationId());
			}
			// Sets the message id.
			if (message.getLastValueKey() != null) {
				jmsMessage.setStringProperty(Message.HDR_LAST_VALUE_NAME.toString(), message.getLastValueKey());
			}
			// Adds each extra properties.
			for (final Map.Entry<String, Object> property : message.getProperties().entrySet()) {
				if (property.getValue() instanceof String) {
					jmsMessage.setStringProperty(property.getKey(), (String) property.getValue());
				}
				else if (property.getValue() instanceof Long) {
					jmsMessage.setLongProperty(property.getKey(), (Long) property.getValue());
				}
				else if (property.getValue() instanceof Integer) {
					jmsMessage.setIntProperty(property.getKey(), (Integer) property.getValue());
				}
				else if (property.getValue() instanceof Integer) {
					jmsMessage.setBooleanProperty(property.getKey(), (Boolean) property.getValue());
				}
				// TODO Other types.
			}
			// Returns the message.
			return jmsMessage;
		});
	}

}
