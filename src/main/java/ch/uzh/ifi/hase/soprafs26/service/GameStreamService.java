package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class GameStreamService {

    private final Map<Long, List<SseEmitter>> gameEmitters = new ConcurrentHashMap<>();

    public SseEmitter addClient(Long gameId) {
        SseEmitter emitter = new SseEmitter(0L);

        gameEmitters.putIfAbsent(gameId, new CopyOnWriteArrayList<>());
        gameEmitters.get(gameId).add(emitter);

        emitter.onCompletion(() -> removeClient(gameId, emitter)); //onCompletion is triggered if the client closes browser, loses connection or through emitter.complete()
        emitter.onTimeout(() -> removeClient(gameId, emitter)); //should not happen, since we have timeout:0, but for safety, because other components/proxy/load balancer etc can trigger a timeout
        emitter.onError(error -> removeClient(gameId, emitter)); //if an error on the conection occurs, we don't use the error, but onError expects a function with parameter error

        return emitter;
    }

    public void sendGameToAllClients(Game game) {
        if (game == null || game.getId() == null) {
            return;
        }

        Long gameId = game.getId();
        List<SseEmitter> emitters = gameEmitters.get(gameId);

        if (emitters == null) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("game-update")
                                .data(DTOMapper.INSTANCE.convertEntityToGameGetDTO(game))
                );
            } catch (Exception e) {
                removeClient(gameId, emitter);
            }
        }
    }

    public void sendGameDeletedToAllClients(Long gameId) {
        List<SseEmitter> emitters = gameEmitters.get(gameId);

        if (emitters == null) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("game-deleted")
                                .data("deleted")
                );
                emitter.complete(); //this triggers onCompletion. 
            } catch (Exception e) {
                removeClient(gameId, emitter);
            }
        }

        gameEmitters.remove(gameId);
    }

    private void removeClient(Long gameId, SseEmitter emitter) {
        List<SseEmitter> emitters = gameEmitters.get(gameId);

        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);

        if (emitters.isEmpty()) {
            gameEmitters.remove(gameId);
        }
    }
}
