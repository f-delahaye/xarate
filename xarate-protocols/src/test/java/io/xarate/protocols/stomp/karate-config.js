function fn() {
    var port = karate.properties['karate.server.port'];
    port = port || '8080';
    return { wsUrl: 'ws://localhost:' + port + '/stomp' };
  }