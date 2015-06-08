package org.westcoasthonorcamp.mac;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Getter;
import lombok.Setter;

@XmlRootElement
public class Music
{
	
	@Getter
	@Setter
	private int id;
	
	@Getter
	@Setter
	private String location;
	
	@Getter
	@Setter
	private String name;
	
}
