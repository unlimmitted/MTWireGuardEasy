package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import ru.unlimmitted.mtwgeasy.dto.TrafficRate
import ru.unlimmitted.mtwgeasy.dto.WgInterface

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.LongStream

@Service
class MikroTikFiles extends MikroTikExecutor {

	private static final String trafficRateFileName = "traffic_rate.txt"

	Boolean isFileExists(String fileName) {
		return !executeCommand("/file/print where name=\"${fileName}\"").isEmpty()
	}

	void saveInterfaceTraffic() {
		String json = "[]"
		if (isFileExists(trafficRateFileName)) {
			if (connect.connected) {
				connect.close()
			}
			initializeConnection()
			json = executeCommand("/file/print where name=\"${trafficRateFileName}\"").contents.first
			String id = executeCommand("/file/print where name=\"${trafficRateFileName}\"")['.id'].first
			executeCommand("/file/remove numbers=$id")
		}
		WgInterface inputInterface = wgInterfaces.find { it.name == settings.inputWgInterfaceName }
		Long tX = inputInterface.txByte.toLong() as Long
		Long rX = inputInterface.rxByte.toLong() as Long
		TrafficRate rate = new TrafficRate(tX, rX, Instant.now())
		ObjectMapper mapper = new ObjectMapper()
		List<TrafficRate> rates = (mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, TrafficRate.class)) as List<TrafficRate>)
				.findAll {
					Instant.ofEpochSecond(it.time) > Instant.now().minus(1, ChronoUnit.HOURS)
				}
		rates.add(rate)
		json = mapper.writeValueAsString(rates)
		executeCommand("/file/add name=\"${trafficRateFileName}\" contents='${json}'")
	}

	List<TrafficRate> getTrafficByMinutes() {
		setWgInterfaces()
		setSettings()
		if (!isFileExists(trafficRateFileName)) {
			saveInterfaceTraffic()
		}
		String json = executeCommand("/file/print where name=\"${trafficRateFileName}\"").contents.first
		ObjectMapper mapper = new ObjectMapper()
		List<TrafficRate> rates = mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, TrafficRate.class))
		return LongStream.range(1, rates.size())
				.mapToObj { i ->
					new TrafficRate(
							(rates[i].tx - rates[i - 1].tx) / 1_048_576 as Long,
							(rates[i].rx - rates[i - 1].rx) / 1_048_576 as Long,
							Instant.ofEpochSecond(rates[i].time)
					)
				}
				.toList()
	}

}
