package studio.v.opcvt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import static android.support.v7.appcompat.R.styleable.View;

public class objchooser extends AppCompatActivity {

    private File dir = new File("/"); //sets a directory at Root
    private Filefilterer filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objchooser);
    }

    //
//
//Filescanner
    //Calls custom class Filefilterer
    /*Filefilterer includes funtions
     accept(File)
     checkDirectory(File)
     checkFileExtension(File)
     getFileExtension(File)
     getfarray(File)
     getnamelist(File)
     Also has a list of all formats
      */
    //
//
    File listed[] = filter.getfarray(dir);
    //listView =findViewById(R.id.file_list);
    final ListView listview = (ListView) findViewById(R.id.file_list);
    String[] files = filter.getnamelist(listed);
    ArrayList<String> arrayList = makelist(files);

    public ArrayList <String> makelist(String[] string) {
        ArrayList<String> list = null;
        for (int i = 0; i < string.length; i++)
            list.add(string[i]);
        return list;
    }

   // final StableArrayAdapter adapter = new StableArrayAdapter(this,R.layout.activity_objchooser,arrayList);
    //listview.setAdapter(adapter);

//
//
}