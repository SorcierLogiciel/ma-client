package org.westcoasthonorcamp.ma.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.westcoasthonorcamp.ma.common.data.Music;
import org.westcoasthonorcamp.ma.common.message.Status;

public class Client
{
	
	public static void main(String[] args)
	{
		new Client("225.0.0.1", 5000).run();
	}
	
	private final Map<Integer, Music> musicMap = new HashMap<>();
	private final PlayerController musicController = new PlayerController();
	private final PlayerController soundController = new PlayerController();
	private final byte[] buffer = new byte[8192];
	private final InetAddress address;
	private final int port;
	private final MulticastSocket socket;
	
	private WebTarget webTarget;
	private Status lastStatus;
	
	public Client(String address, int port)
	{
		try
		{
			
			this.address = InetAddress.getByName(address);
			this.port = port;
			socket = new MulticastSocket(port);
			socket.joinGroup(this.address);
			
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void run()
	{
		
		soundController.playResource("listening.mp3", true, 500, System.currentTimeMillis());
		while(true)
		{
			
			try
			{
				handleStatus(readStatus());
			}
			catch(IOException | NotFoundException e)
			{
				soundController.stop();
				soundController.playResource("listening.mp3", true, 500, System.currentTimeMillis());
			}
			catch(ClassNotFoundException e)
			{
				throw new RuntimeException(e);
			}
			
		}
		
	}
	
	private Status readStatus() throws IOException, ClassNotFoundException
	{
		
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
		socket.receive(packet);
		
		ByteArrayInputStream baiStream = new ByteArrayInputStream(buffer);
		ObjectInputStream oiStream = new ObjectInputStream(baiStream);
		Status newStatus = (Status) oiStream.readObject();
		oiStream.close();
		
		return newStatus;
		
	}
	
	private void handleStatus(Status newStatus)
	{

		if(lastStatus != null && !lastStatus.getServerUrl().equals(newStatus.getServerUrl()))
		{
			return;
		}
		
		if(lastStatus == null || (lastStatus.getSystemUpdateId() != newStatus.getSystemUpdateId() && newStatus.isReload()))
		{

			soundController.stop();
			soundController.playResource("loading.mp3", true, 500, System.currentTimeMillis());
			if(webTarget == null)
			{
				webTarget = ClientBuilder.newClient().target(newStatus.getServerUrl());
			}
			loadMusic();
			soundController.stop();
			soundController.playResource("completed.mp3");
			
		}
		
		if(lastStatus == null || lastStatus.getMusicUpdateId() != newStatus.getMusicUpdateId())
		{
			
			if(newStatus.isOverride() || newStatus.getMusicId() == 0)
			{
				musicController.stop();
			}
			
			if(newStatus.getMusicId() != 0)
			{
				musicController.play(Paths.get(musicMap.get(newStatus.getMusicId()).getLocation()), false, 0, newStatus.getStartTime());
			}
		
		}
		
		lastStatus = newStatus;
		
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
