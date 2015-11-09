package cc.openframeworks.plugin.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.osgi.framework.Bundle;

import cc.openframeworks.plugin.Activator;

public class SelectAddonPage extends WizardPage{
	private Composite container;
	private Tree addonsList;
	private Tree officialAddonsList;
	private Set<String> currentAddons = new HashSet<String>();
	private Set<String> officialAddons = new HashSet<String>();
	private Set<String> invalidAddons = new HashSet<String>();
	
	public SelectAddonPage() {
		super("Select addon");

	    setTitle("Add openFrameworks addon");
	    Bundle bundle = Platform.getBundle("cc.openframeworks.plugin");
	    setImageDescriptor(ImageDescriptor.createFromURL(bundle.getEntry("icons/ofw-logo.png")));
	    setDescription("Select an addon from the list");
	    String officials[] = new String[]{
			"ofx3DModelLoader",
			"ofxAssimpModelLoader",
			"ofxGui",
			"ofxKinect",
			"ofxNetwork",
			"ofxOpenCv",
			"ofxOsc",
			"ofxSvg",
			"ofxThreadedImageLoader",
			"ofxVectorGraphics",
			"ofxXmlSettings",
	    };
	    Collections.addAll(officialAddons, officials);
	    String invalids[] = new String[]{
    		"ofxAndroid",
    		"ofxMultitouch",
    		"ofxAccelerometer",
    		"ofxiOS",
	    };
	    Collections.addAll(invalidAddons, invalids);
	}

	@Override
	public void createControl(Composite parent) {

		container = new Composite(parent, SWT.NONE);
	    GridLayout layout = new GridLayout();
	    container.setLayout(layout);
	    layout.marginBottom = 0;
	    layout.numColumns = 1;
	    

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		container.setLayoutData(data);
	    
	    Label officialAddonsLabel = new Label(container,SWT.NONE);
	    officialAddonsLabel.setText("Official addons");

	    officialAddonsList = new Tree(container, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER);
	    officialAddonsList.setHeaderVisible(false);

	    Label addonsLabel = new Label(container,SWT.NONE);
	    addonsLabel.setText("Community addons");
	    
	    addonsList = new Tree(container, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER);
	    addonsList.setHeaderVisible(false);
	    

	    GridData otherGrid = new GridData(GridData.FILL_BOTH);
	    otherGrid.heightHint = 8 * officialAddonsList.getItemHeight();
	    officialAddonsList.setLayoutData(otherGrid);
	    GridData myGrid = new GridData(GridData.FILL_BOTH);
	    myGrid.heightHint = 8 * addonsList.getItemHeight();
	    addonsList.setLayoutData(myGrid);

		String ofRoot = Activator.getDefault().getPreferenceStore().getString("OF_ROOT");
		if(ofRoot!=null && ofRoot!=""){
		    File addonsFolder = new File(ofRoot + "/addons");
		    ArrayList<String> addonsNames = new ArrayList<String>();
		    for(File f:addonsFolder.listFiles()){
		    	if(f.isDirectory() && f.getName().startsWith("ofx")){
		    		addonsNames.add(f.getName());
		    	}
		    } 
		    addonsNames.sort(new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return o1.compareTo(o2);
				}
			});

		    for(String f: addonsNames){
		    	if(invalidAddons.contains(f)){
		    		continue;
		    	}
		    	TreeItem item;
		    	if(officialAddons.contains(f)){
		    		item = new TreeItem(officialAddonsList, SWT.NONE);
		    	}else{
		    		item = new TreeItem(addonsList, SWT.NONE);
		    	}
		    	item.setText(f);
	    		if(currentAddons.contains(f)){
	    			item.setChecked(true);
	    		}
		    }
		}else{
			setMessage("Can't parse avaliable addons until openFrameworks path is set in Windows > Preferences > openFrameworks",WizardPage.WARNING);
		}
	    
	    
	    setControl(container);
	}
	
	public String[] getSelectedAddons(){
		ArrayList<String> names = new ArrayList<String>();
		for(TreeItem item: addonsList.getItems()){
			if(item.getChecked()){
				names.add(item.getText());
			}
		}
		for(TreeItem item: officialAddonsList.getItems()){
			if(item.getChecked()){
				names.add(item.getText());
			}
		}
		return names.toArray(new String[names.size()]);
	}

	public void setAddons(Set<String> currentAddons) {
		this.currentAddons = currentAddons;
		if(addonsList!=null){
			for(TreeItem item: addonsList.getItems()){
	    		if(currentAddons.contains(item.getText())){
	    			item.setChecked(true);
	    		}else{
	    			item.setChecked(false);
	    		}
			}
		}
		if(officialAddonsList!=null){
			for(TreeItem item: officialAddonsList.getItems()){
	    		if(currentAddons.contains(item.getText())){
	    			item.setChecked(true);
	    		}else{
	    			item.setChecked(false);
	    		}
			}
		}
	}

}
