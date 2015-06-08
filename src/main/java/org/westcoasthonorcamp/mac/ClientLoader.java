package org.westcoasthonorcamp.mac;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

public class ClientLoader
{
	
	public static void main(String[] args)
	{
		new ClientLoader("225.0.0.1", 5000);
	}
	
	private final Map<Integer, Music> musicMap = new HashMap<>();
	private final PlayerController playerController = new PlayerController();
	private final WebTarget webTarget;
	
	public ClientLoader(String address, int port)
	{
		
		String serverUrl = findServerUrl(address, port);
		webTarget = ClientBuilder.newClient().target(serverUrl);
		playerController.playResource("loading.mp3", true, 5000);
		loadMusic();
		playerController.stop();
		playerController.playResource("completed.mp3");
		
	}
	
	private String findServerUrl(String address, int port)
	{
		
		try(MulticastSocket socket = new MulticastSocket(port))
		{
			
			byte[] buffer = new byte[4096];
			InetAddress inetAddress = InetAddress.getByName(address);
			socket.joinGroup(inetAddress);
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inetAddress, port);
			socket.receive(packet);
			return new String(buffer).trim();
			
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		
	}

	private void loadMusic()
	{
		
		musicMap.clear();
		for(Music m : webTarget.path("music").request(MediaType.APPLICATION_XML).get(new GenericType<List<Music>>() {}))
		{
			
			byte[] bytes = webTarget.path("music/" + m.getId() + "/file").request(MediaType.APPLICATION_OCTET_STREAM).get(byte[].class);
			try
			{
				
				Path p = File.createTempFile("mac-", "-" + m.getName() + ".mp3").toPath();
				Files.write(p, bytes, StandardOpenOption.WRITE);
				m.setLocation(p.toString());
				musicMap.put(m.getId(), m);
				
			}
			catch(IOException e)
			{
				throw new RuntimeException(e);
			}
			
		}
		
	}
	
}
