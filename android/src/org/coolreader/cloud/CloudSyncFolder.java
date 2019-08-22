package org.coolreader.cloud;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.ChooseBookmarksDlg;
import org.coolreader.crengine.ChooseConfFileDlg;
import org.coolreader.crengine.ChooseReadingPosDlg;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.zip.CRC32;

public class CloudSyncFolder {

    public static int CLOUD_SAVE_READING_POS = 1;
    public static int CLOUD_SAVE_BOOKMARKS = 2;

    private static final String TAG = "CloudSyncFolder";

    public static void saveSettingsFiles(CoolReader cr, boolean bQuiet) {
        int iSettClount = 1;
        ArrayList<File> arrSett = new ArrayList<File>();
        File fSett = cr.getSettingsFile(0);
        while (fSett.exists()) {
            arrSett.add(fSett);
            fSett = cr.getSettingsFile(iSettClount);
            iSettClount++;
        }
        ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CloudSyncDirs, true);
        String sDir = "";
        if (tDirs.size() > 0) sDir = tDirs.get(0);
        if (!StrUtils.isEmptyStr(sDir))
            if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
        boolean bWasErr = false;
        if (!StrUtils.isEmptyStr(sDir)) {
            Log.d(TAG, "Starting save cr3.ini files to drive...");
            final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
            final CharSequence sFName0 = dfmt.format("yyyy-MM-dd_kkmmss", new java.util.Date());
            for (File fS : arrSett) {
                final String sFName = sFName0.toString() + "_" + fS.getName() + "_" + cr.getAndroid_id();
                try {
                    FileInputStream fin = new FileInputStream(fS);
                    byte[] buffer = new byte[fin.available()];
                    fin.read(buffer, 0, buffer.length);
                    File f = new File(sDir + sFName);
                    if (f.exists()) f.delete();
                    File file = new File(sDir + sFName);
                    OutputStream fOut = new FileOutputStream(file);
                    fOut.write(buffer, 0, buffer.length);
                    fOut.flush();
                    fOut.close();
                } catch (Exception e) {
                    bWasErr = true;
                    if (!bQuiet)
                        cr.showToast(cr.getString(R.string.cloud_error) + ": Error saving file (" + e.getClass().getSimpleName() + ")");
                }
            } //for (File fS : arrSett) {
            try {
                final String sFName = sFName0.toString() + "_cr3_ini_" + cr.getAndroid_id();
                File f2 = new File(sDir + sFName + ".info");
                if (f2.exists()) f2.delete();
                File file2 = new File(sDir + sFName + ".info");
                OutputStream fOut2 = new FileOutputStream(file2);
                String descr = String.valueOf(arrSett.size()) + " profiles ~from " + cr.getModel();
                fOut2.write(descr.getBytes(), 0, descr.getBytes().length);
                fOut2.flush();
                fOut2.close();
            } catch (Exception e) {
                bWasErr = true;
                if (!bQuiet)
                    cr.showToast(cr.getString(R.string.cloud_error) + ": Error saving file (" + e.getClass().getSimpleName() + ")");
            }
        }
    }

    public static void loadSettingsFiles(CoolReader cr, boolean bQuiet) {
        final File fSett = cr.getSettingsFile(0);
        Log.d(TAG, "Starting load settings files from drive...");
        ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CloudSyncDirs, true);
        String sDir = "";
        if (tDirs.size() > 0) sDir = tDirs.get(0);
        if (!StrUtils.isEmptyStr(sDir))
            if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
        if (!StrUtils.isEmptyStr(sDir)) {
            File fDir = new File(sDir);
            File[] matchingFilesInfo = fDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.contains("_cr3_ini_") && (name.endsWith(".info"));
                }
            });
            File[] matchingFiles = fDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.contains("_cr3.ini");
                }
            });
            ChooseConfFileDlg dlg = new ChooseConfFileDlg(cr, matchingFilesInfo, matchingFiles);
            dlg.show();
        }
    }

    public static void restoreSettingsFiles(CoolReader cr, CloudFileInfo fi, ArrayList<CloudFileInfo> afi, boolean bQuiet) {
        Log.d(TAG, "Starting load settings file from drive...");
        boolean bWasErr = false;
        for (CloudFileInfo fiSett: afi) {
            int profileNum = 0;
            try {
                if (fiSett.name.contains("profile")) {
                    for (String s : fiSett.name.split("_")) {
                        if (s.contains("profile"))
                            profileNum = Integer.valueOf(s.replace("cr3.ini.profile", ""));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            File fSett = cr.getSettingsFile(profileNum);
            String sFName = fSett.getPath();
            File file = new File(sFName + ".bk");
            if (file.exists()){
                if (!file.delete()) {
                    if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_error) + ": delete .bk file");
                    bWasErr = true;
                };
            }
            if (!bWasErr) {
                try {
                    Utils.copyFile(sFName, sFName + ".bk");
                } catch (Exception e) {
                    bWasErr = true;
                    if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_error) + ": copy current file to .bk");
                }
            }
            if (!bWasErr) {
                if (fSett.exists()) {
                    if (!fSett.delete()) {
                        if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_error) + ": delete "+fSett.getName()+" file");
                        bWasErr = true;
                    };
                }
            }
            if (!bWasErr) {
                try {
                    Utils.copyFile(fiSett.path, sFName);
                } catch (Exception e) {
                    bWasErr = true;
                    if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_error) + ": copy new file to current");
                }
            }
        }
        if (!bWasErr) {
            if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_ok) + ": Settings file(s) were restored. Closing app");
            cr.finish();
        } else {
            if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_error) + ": Some errors occured while restoring setting - you may need to check backup files. Closing app");
            cr.finish();
        }
    }

    public static void saveJsonInfoFile(CoolReader cr, int iSaveType, boolean bQuiet) {
        Log.d(TAG, "Starting save json to drive...");

        final ReaderView rv = cr.getReaderView();
        if (rv == null) {
            if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_error) + ": book was not found");
            return;
        }

        ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CloudSyncDirs, true);
        String sDir = "";
        if (tDirs.size() > 0) sDir = tDirs.get(0);
        if (!StrUtils.isEmptyStr(sDir))
            if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
        if (!StrUtils.isEmptyStr(sDir)) {
            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String prettyJson = "";

            Bookmark bmk = null;
            ArrayList<Bookmark> abmk = null;
            String fileMark = "";
            String descr = "";
            final String sBookFName = rv.getBookInfo().getFileInfo().filename;
            if (iSaveType == CLOUD_SAVE_READING_POS) {
                // delete old files
                CRC32 crc = new CRC32();
                crc.update(sBookFName.getBytes());
                final String sCRC = String.valueOf(crc.getValue());
                File fDir = new File(sDir);
                File[] matchingFiles = fDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.contains("_rpos_")&&(name.contains(sCRC));
                    }
                });
                ArrayList<File> mReadingPosList = new ArrayList<File>();
                for (File f: matchingFiles) mReadingPosList.add(f);

                Comparator<File> compareByDate = new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return -(o1.getName().compareTo(o2.getName()));
                    }
                };
                Collections.sort(mReadingPosList, compareByDate);
                for (int i=0;i<40;i++) {
                    if (mReadingPosList.size()>0) mReadingPosList.remove(0);
                }
                for (File f: mReadingPosList) f.delete();
                // continue
                fileMark = "_rpos_";
                bmk = rv.getCurrentPositionBookmark();
                if (bmk == null) {
                    if (!bQuiet)
                        cr.showToast(cr.getString(R.string.cloud_error) + ": pos was not got");
                    return;
                }
                prettyJson = gson.toJson(bmk);
                double dPerc = bmk.getPercent();
                dPerc = dPerc / 100;
                final String sPerc = String.format("%5.2f", dPerc) + "% of ";
                descr = sPerc + sBookFName + " ~from " + cr.getModel();
            }
            if (iSaveType == CLOUD_SAVE_BOOKMARKS) {
                fileMark = "_bmk_";
                abmk = rv.getBookInfo().getAllBookmarksWOPos();
                if (abmk == null) {
                    if (!bQuiet)
                        cr.showToast(cr.getString(R.string.cloud_error) + ": bookmarks were not got");
                    return;
                }
                if (abmk.isEmpty()) return;
                prettyJson = gson.toJson(abmk);
                descr = String.valueOf(abmk.size()) + " bookmark(s) of " + sBookFName + " ~from " + cr.getModel();
            }
            CRC32 crc = new CRC32();
            crc.update(sBookFName.getBytes());
            final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
            final CharSequence sFName0 = dfmt.format("yyyy-MM-dd_kkmmss", new java.util.Date());

            final String sFName = sFName0.toString() + fileMark + String.valueOf(crc.getValue()) + "_" + cr.getAndroid_id();

            boolean bWasErr = false;
            try {
                File f = new File(sDir + sFName + ".json");
                if (f.exists()) f.delete();
                File file = new File(sDir + sFName + ".json");
                OutputStream fOut = new FileOutputStream(file);
                fOut.write(prettyJson.getBytes(), 0, prettyJson.getBytes().length);
                fOut.flush();
                fOut.close();
                File f2 = new File(sDir + sFName + ".info");
                if (f2.exists()) f2.delete();
                File file2 = new File(sDir + sFName + ".info");
                OutputStream fOut2 = new FileOutputStream(file2);
                fOut2.write(descr.getBytes(), 0, descr.getBytes().length);
                fOut2.flush();
                fOut2.close();
            } catch (Exception e) {
                bWasErr = true;
                if (!bQuiet)
                    cr.showToast(cr.getString(R.string.cloud_error) + ": Error saving file (" + e.getClass().getSimpleName() + ")");
            }
        }
    }

    public static void loadFromJsonInfoFileList(CoolReader cr, int iSaveType, boolean bQuiet) {
        Log.d(TAG, "Starting load json file list from drive...");

        final ReaderView rv = cr.getReaderView();
        if (rv == null) {
            if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_error) + ": book was not found");
            return;
        }

        final String sBookFName = rv.getBookInfo().getFileInfo().filename;
        CRC32 crc = new CRC32();
        crc.update(sBookFName.getBytes());
        final String sCRC = String.valueOf(crc.getValue());

        ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CloudSyncDirs, true);
        String sDir = "";
        if (tDirs.size() > 0) sDir = tDirs.get(0);
        if (!StrUtils.isEmptyStr(sDir))
            if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
        if (!StrUtils.isEmptyStr(sDir)) {
            File fDir = new File(sDir);
            if (iSaveType == CLOUD_SAVE_READING_POS) {
                File[] matchingFiles = fDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.contains("_rpos_")&&(name.endsWith(".json"))&&(name.contains(sCRC));
                    }
                });
                ChooseReadingPosDlg dlg = new ChooseReadingPosDlg(cr, matchingFiles);
                dlg.show();
            }
            if (iSaveType == CLOUD_SAVE_BOOKMARKS) {
                File[] matchingFiles = fDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.contains("_bmk_")&&(name.endsWith(".json"))&&(name.contains(sCRC));
                    }
                });
                ChooseBookmarksDlg dlg = new ChooseBookmarksDlg(cr, matchingFiles);
                dlg.show();
            }
        }
    }

    public static void loadFromJsonInfoFile(CoolReader cr, int iSaveType, String filePath, boolean bQuiet) {
        final File fSett = cr.getSettingsFile(0);
        Log.d(TAG, "Starting load json file from drive...");

        final ReaderView rv = cr.getReaderView();
        if (rv == null) {
            if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_error) + ": book was not found");
            return;
        }

        String sFile = Utils.readFileToString(filePath);
        if (StrUtils.isEmptyStr(sFile)) {
            if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_error) + ": json file was not found");
            return;
        }
        if (iSaveType == CLOUD_SAVE_READING_POS) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Bookmark bmk = gson.fromJson(sFile, Bookmark.class);
            if (bmk != null) {
                bmk.setTimeStamp(System.currentTimeMillis());
                rv.savePositionBookmark(bmk);
                if ( rv.getBookInfo()!=null )
                    rv.getBookInfo().setLastPosition(bmk);
                rv.goToBookmark(bmk);
                if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_ok) + ": reading pos updated");
            }
        }
        if (iSaveType == CLOUD_SAVE_BOOKMARKS) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            ArrayList<Bookmark> abmkThis = rv.getBookInfo().getAllBookmarks();
            ArrayList<Bookmark> abmk = new ArrayList<Bookmark>(StrUtils.stringToArray(sFile, Bookmark[].class));
            int iCreated = 0;
            for (Bookmark bmk: abmk) {
                boolean bFound = false;
                for (Bookmark bm: abmkThis) {
                    if (bm.getStartPos().equals(bmk.getStartPos())) bFound = true;
                }
                if ((!bFound)&(bmk.getType()!=Bookmark.TYPE_LAST_POSITION)) {
                    rv.getBookInfo().addBookmark(bmk);
                    iCreated++;
                }
            }
            rv.highlightBookmarks();
            if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_ok) + ": " + iCreated + " bookmark(s) created");
        }
    }
}