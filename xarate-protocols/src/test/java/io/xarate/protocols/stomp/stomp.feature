Feature: stomp

Scenario: one client
  * def StompClient = Java.type('io.xarate.protocols.stomp.StompNettyClient')
  * def client1 = new StompClient(wsUrl);
  * client1.subscribe('topic/message1').listen('20000');
  * client1.close()

