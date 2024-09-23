package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import ru.unlimmitted.mtwgeasy.dto.TrafficRate

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.LongStream

@Service
class MikroTikFiles extends MikroTikExecutor {

	private static final String trafficRateFileName = "traffic_rate.txt"

	Boolean isFileExists(String fileName) {
		List<Map<String, String>> files = executeCommand('/file/print')
		return files.find { it.name == fileName } != null
	}

	void saveInterfaceTraffic() {
		String json = "[]"
		if (isFileExists(trafficRateFileName)) {
			if (connect.connected) {
				connect.close()
			}
			initializeConnection()
			json = executeCommand('/file/print')
					.find {
						it.name == trafficRateFileName
					}?.contents
			Integer number = executeCommand("/file/print")
					.indexed()
					.find { index, it -> it.name == trafficRateFileName }
					.key
			executeCommand("/file/remove numbers=$number")
		}
		Long tX = wgInterfaces.find { it.name == settings.inputWgInterfaceName }.txByte.toLong() as Long
		Long rX = wgInterfaces.find { it.name == settings.inputWgInterfaceName }.rxByte.toLong() as Long
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
		String json = ""
		if (!isFileExists(trafficRateFileName)) {
			saveInterfaceTraffic()
		}
		json = executeCommand('/file/print')
				.find { it.name == trafficRateFileName }
				?.contents
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
