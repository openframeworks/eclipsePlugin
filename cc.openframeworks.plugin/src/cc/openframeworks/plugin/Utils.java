package cc.openframeworks.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class Utils {
	public static Set<String> parseAddonsMake(File addonsMake){		
		Set<String> currentAddons = new HashSet<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(addonsMake));
			String line;
			while ((line = br.readLine().trim()) != null) {
				if(line.startsWith("#")){
					continue;
				}
				if(line.contains("#")){
					line = line.split("#")[0].trim();
				}
			    currentAddons.add(line);
			}
			br.close();
		} catch (Exception e1) {
		}
		return currentAddons;
	}
	
	public static void reparseAddons(IProject project, String[] newAddons, String ofRoot) throws CoreException{
		File projectFolder = project.getLocation().toFile();
		File addonsMake = new File(projectFolder.getAbsolutePath() + "/addons.make");
		if(!addonsMake.exists()){
			try {
				addonsMake.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Set<String> currentAddons = parseAddonsMake(addonsMake);
		
		try {
			IFolder addonsFolder = project.getFolder("addons");
			if(!addonsFolder.exists()){
				addonsFolder.createLink(new URI("virtual:/virtual"), IResource.ALLOW_MISSING_LOCAL, null);
			}
			
			for(String addon: currentAddons){
				boolean found = false;
				for(String newAddon: newAddons){
					if(addon.equals(newAddon)){
						found = true;
						break;
					}
						
					if(!found){
						IFolder folder = project.getFolder("addons/"+addon);
						folder.delete(true, null);
					}
				}
			}
			
			if(ofRoot==null || ofRoot.equals("")){
				ofRoot="${PARENT-3-PROJECT_LOC}";
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(addonsMake));
			for(String addon: newAddons){
				bw.write(addon + "\n");
				IFolder folder = project.getFolder("addons/"+addon);
				if(!folder.exists()){
					try {
						folder.createLink(new URI(ofRoot + "/addons/"+addon), IResource.ALLOW_MISSING_LOCAL, null);
					} catch (URISyntaxException e) {
						throwCoreException(e.getMessage());
					}
				}
			}
			bw.close();
		} catch (URISyntaxException e) {
			throwCoreException(e.getMessage());
		} catch (IOException e) {
			throwCoreException(e.getMessage());
		}
	}

	public static void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "cc.openframeworks.plugin", IStatus.OK, message, null);
		throw new CoreException(status);
	}
}
