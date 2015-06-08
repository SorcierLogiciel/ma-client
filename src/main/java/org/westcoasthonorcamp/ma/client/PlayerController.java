package org.westcoasthonorcamp.ma.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;


public class PlayerController
{
	
	private ExecutorService playerExecutor = Executors.newSingleThreadExecutor();
	private Player player;
	
	public void playResource(String resourceName)
	{
		playResource(resourceName, false, 0);
	}
	
	public void playResource(String resourceName, boolean loop, int delay)
	{
		
		try
		{
			
			Path tempFile = File.createTempFile("mac-", "-" + resourceName).toPath();
			Files.copy(getClass().getClassLoader().getResourceAsStream(resourceName), tempFile, StandardCopyOption.REPLACE_EXISTING);
			play(tempFile, loop, delay);
			
		}
		catch(IOException e)
		{
			System.out.println("Unable to play resource");
		}
		
	}
	
	public void play(final Path location)
	{
		play(location, false, 0);
	}
	
	public synchronized void play(final Path location, final boolean loop, final int delay)
	{
		
		if(location != null && (player == null || player.isComplete())) //Override
		{
			
			if(player != null)
			{
				player.close();
			}
						
			playerExecutor.execute(new Runnable()
			{
				
				@Override
				public void run()
				{
				
					try
					{

						do
						{
							
							System.out.println(String.format("Started playing %s", location));
							player = new Player(Files.newInputStream(location));
							player.play();
							Thread.sleep(delay);
							
						} while(loop);
						
					}
					catch(JavaLayerException | IOException e)
					{
						System.out.println(String.format("Unable to play %s", location));
					}
					catch(InterruptedException e)
					{
						System.out.println(String.format("Stopped playing %s", location));
					}
					
				}
				
			});
			
		}
		
	}
	
	public synchronized void stop()
	{
		
		if(player != null)
		{
			player.close();
			player = null;
		}
		playerExecutor.shutdownNow();
		playerExecutor = Executors.newSingleThreadExecutor();
		
	}
	
}
