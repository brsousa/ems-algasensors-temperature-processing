package com.algaworks.algasensors.temperature.processing.api.controller;

import com.algaworks.algasensors.temperature.processing.api.model.TemperatureLogOutput;
import com.algaworks.algasensors.temperature.processing.common.IdGenerator;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static com.algaworks.algasensors.temperature.processing.infrastructure.rabbitmq.RabbitMQConfig.FANNOUT_EXCHANGE_NAME;

@RestController
@RequestMapping("/api/sensors/{sensorId}/temperatures/data")
@Slf4j
@RequiredArgsConstructor
public class TemperatureProcessingController {

    private final RabbitTemplate rabbitTemplate;

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public void data(@PathVariable TSID sensorId, @RequestBody String input) {
        if(input == null || input.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Double temperature;
        temperature = validateNumberFormatToGetTemperature(input);
        TemperatureLogOutput logOutput = buildOutputResponse(sensorId, temperature);
        log.info(logOutput.toString());

        String exchange = FANNOUT_EXCHANGE_NAME;
        String routingKey = "";
        Object payload = logOutput;

        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }

    private static TemperatureLogOutput buildOutputResponse(TSID sensorId, Double temperature) {
        return TemperatureLogOutput.builder()
                .id(IdGenerator.generateTimeBasedUUID())
                .sensorId(sensorId)
                .value(temperature)
                .registeredAt(OffsetDateTime.now())
                .build();
    }

    private static Double validateNumberFormatToGetTemperature(String input) {
        Double temperature;
        try {
            temperature = Double.parseDouble(input);
        } catch (NumberFormatException nfe) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return temperature;
    }

}
