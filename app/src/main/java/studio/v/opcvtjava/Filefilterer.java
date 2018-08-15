package studio.v.opcvtjava;

//start

import android.net.Uri;
import android.util.Log;

import java.io.FileFilter;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;

public class Filefilterer implements FileFilter {

    Log LogHelper;
        protected static final String TAG = "FileFilter";
        /**
         * allows Directories
         */
        private final boolean allowDirectories;
        public Filefilterer( boolean allowDirectories) {
            this.allowDirectories = allowDirectories;}
        //construct
        public Filefilterer() {
            this(true);
        }
        @Override
        public boolean accept(File f) {
            if ( f.isHidden() || !f.canRead() ) {
                return false;
            }
            if ( f.isDirectory() ) {
                return checkDirectory( f );
            }
            return checkFileExtension( f );
        }
        public boolean checkFileExtension( File f ) {
            String ext = getFileExtension(f);
            if ( ext == null) return false;
            try {
                if ( SupportedFileFormat.valueOf(ext.toUpperCase()) != null ) {
                    return true;
                }
            } catch(IllegalArgumentException e) {
                //Not known enum value
                return false;
            }
            return false;
        }

        private boolean checkDirectory( File dir ) {
            if ( !allowDirectories ) {
                return false;
            } else {
                final ArrayList<File> subDirs = new ArrayList<File>();
                int Numb = dir.listFiles( new FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        if ( file.isFile() ) {
                            if ( file.getName().equals( ".nomedia" ) )
                                return false;

                            return checkFileExtension( file );
                        } else if ( file.isDirectory() ){
                            subDirs.add( file );
                            return false;
                        } else
                            return false;
                    }
                } ).length;

                if ( Numb > 0 ) {
                    LogHelper.i(TAG, "checkDirectory: dir " + dir.toString() + " return true con songNumb -> " + Numb );
                    return true;
                }

                for( File subDir: subDirs ) {
                    if ( checkDirectory( subDir ) ) {
                        LogHelper.i(TAG, "checkDirectory [for]: subDir " + subDir.toString() + " return true" );
                        return true;
                    }
                }
                return false;
            }
        }

        public boolean checkFileExtension( String fileName ) {
            String ext = getFileExtension(fileName);
            if ( ext == null) return false;
            try {
                if ( SupportedFileFormat.valueOf(ext.toUpperCase()) != null ) {
                    return true;
                }
            } catch(IllegalArgumentException e) {
                //Not known enum value
                return false;
            }
            return false;
        }

        public String getFileExtension( File f ) {
            return getFileExtension( f.getName() );
        }

        public String getFileExtension( String fileName ) {
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                return fileName.substring(i+1);
            } else
                return null;
        }
        //abstracted uri getter
        protected Uri uriget(File file){
            Uri uri = Uri.fromFile(file);
            return uri;
        }
        /**
         * Files formats currently supported by Library
         */
        public enum SupportedFileFormat
        {
            _OBJ("obj"),
            HSEM("hsem");
            //hsem is a placeholder format for future compatibility

            private String filesuffix;

            SupportedFileFormat( String filesuffix ) {
                this.filesuffix = filesuffix;
            }

            public String getFilesuffix() {
                return filesuffix;
            }
        }
    public File[] getfarray(File dir)
    {
        File[] numb=dir.listFiles();
        return numb;
    }
    public String[] getnamelist (File[] files){

        String names[] = new String[files.length];
        for(int i=0;i<files.length;i++){
         File temp = files[i];}
        //temp.toURI()         names[i] = temp.getName();}
    return names;}

}




