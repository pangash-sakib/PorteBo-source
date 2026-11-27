package com.sakib.portebo.ui;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import com.sakib.portebo.security.ParentControlManager;
import android.content.ClipData;import android.content.ClipboardManager;import android.content.Intent;import android.net.Uri;import android.os.Bundle;import android.text.Editable;import android.text.TextWatcher;import android.view.LayoutInflater;import android.view.View;import android.view.ViewGroup;import android.widget.EditText;import android.widget.TextView;import android.widget.ImageButton;import android.widget.ImageView;import android.widget.Toast;import androidx.annotation.NonNull;import androidx.annotation.Nullable;import androidx.appcompat.app.AlertDialog;import androidx.fragment.app.Fragment;import com.google.android.material.button.MaterialButton;import com.google.android.material.dialog.MaterialAlertDialogBuilder;import com.google.android.material.textfield.TextInputEditText;import com.google.android.material.textfield.TextInputLayout;import com.sakib.portebo.R;import com.sakib.portebo.data.PrefsManager;import com.sakib.portebo.message.MessageManager;import com.sakib.portebo.update.UpdateManager;import java.util.ArrayList;import java.util.List;import java.util.concurrent.ExecutorService;import java.util.concurrent.Executors;
public class AboutFragment extends Fragment {private int taps;private long firstTap;private String category="";private EditText feedback;private MaterialButton send;private Uri selectedFeedbackImage;private ImageButton attachImage;private ImageView feedbackPreview;private TextView imageHint,removeImage;private com.google.android.material.chip.Chip chipIssue,chipSuggestion,chipFeedback;private View userCard,devCard,permissionAccessibility,permissionOverlay;private static final int LOGIN=731;
 @Nullable public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle b){View v=i.inflate(R.layout.fragment_about,c,false);TextView version=v.findViewById(R.id.tvAboutVersion);String n=UpdateManager.getCurrentVersionName(requireContext());version.setText("ভার্সন "+n+" (Build "+UpdateManager.getCurrentVersionCode(requireContext())+")");version.setOnClickListener(x->tapVersion());userCard=v.findViewById(R.id.cardUserFeedback);devCard=v.findViewById(R.id.cardDevTools);feedback=v.findViewById(R.id.etFeedbackInput);send=v.findViewById(R.id.btnSendFeedback);attachImage=v.findViewById(R.id.btnAttachFeedbackImage);feedbackPreview=v.findViewById(R.id.ivFeedbackImagePreview);imageHint=v.findViewById(R.id.tvFeedbackImageHint);removeImage=v.findViewById(R.id.btnRemoveFeedbackImage);setMode();setupFeedback(v);setupPermissionGuide(v);v.findViewById(R.id.btnInstagram).setOnClickListener(x->openInstagram());v.findViewById(R.id.btnInstagramUser).setOnClickListener(x->openInstagram());v.findViewById(R.id.btnLogoutDev).setOnClickListener(x->{PrefsManager.setDevModeEnabled(false);requireContext().getSharedPreferences("dev_auth",0).edit().clear().apply();setMode();Toast.makeText(requireContext(),"লগআউট হয়েছে।",Toast.LENGTH_SHORT).show();});v.findViewById(R.id.btnDevInbox).setOnClickListener(x->startActivity(new Intent(requireContext(),DevInboxActivity.class)));v.findViewById(R.id.btnDebugLog).setOnClickListener(x->showDebugLog());v.findViewById(R.id.btnPublishRelease).setOnClickListener(x->showPublishDialog());MaterialButton updates=v.findViewById(R.id.btnCheckUpdates);updates.setOnClickListener(x->{updates.setEnabled(false);updates.setText("চেক হচ্ছে…");UpdateManager.checkForUpdate(requireContext(),(has,ver,notes)->{if(!isAdded())return;updates.setEnabled(true);updates.setText("আপডেট চেক করো");if(has)showUpdateDialog(ver,notes);else Toast.makeText(requireContext(),"তুমি আপডেটেড আছো।",Toast.LENGTH_SHORT).show();});});setupThemePalette(v);return v;}


 private void setupPermissionGuide(View root){
  permissionAccessibility=root.findViewById(R.id.btnAccessibility);
  permissionOverlay=root.findViewById(R.id.btnOverlaySettings);
  permissionAccessibility.setOnClickListener(v->{
   if(!ParentControlManager.requireEditAccess(requireContext()))return;
   openSettings(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
  });
  permissionOverlay.setOnClickListener(v->{
   if(!ParentControlManager.requireEditAccess(requireContext()))return;
   Intent in=new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+requireContext().getPackageName()));
   openSettings(in);
  });
  refreshPermissionGuideAccess();
 }
 private void refreshPermissionGuideAccess(){
  if(permissionAccessibility==null||permissionOverlay==null||!isAdded())return;
  boolean allowed=ParentControlManager.canEdit(requireContext());
  permissionAccessibility.setEnabled(allowed);
  permissionOverlay.setEnabled(allowed);
  permissionAccessibility.setAlpha(allowed?1f:.45f);
  permissionOverlay.setAlpha(allowed?1f:.45f);
 }
 @Override public void onResume(){super.onResume();refreshPermissionGuideAccess();}


 private void setupThemePalette(View root){
  LinearLayout grid=root.findViewById(R.id.layoutThemePalette);
  if(grid==null)return;
  grid.removeAllViews();
  ThemePaletteManager.Palette[] palettes=ThemePaletteManager.getPalettes();
  String current=PrefsManager.getThemePaletteId();
  for(int rowIndex=0;rowIndex<2;rowIndex++){
   LinearLayout row=new LinearLayout(requireContext());
   row.setGravity(Gravity.CENTER);
   LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);
   if(rowIndex>0)rowParams.topMargin=dp(8);
   grid.addView(row,rowParams);
   for(int column=0;column<4;column++){
    final ThemePaletteManager.Palette palette=palettes[rowIndex*4+column];
    final boolean selected=palette.id.equals(current);
    LinearLayout item=new LinearLayout(requireContext());
    item.setOrientation(LinearLayout.VERTICAL);
    item.setGravity(Gravity.CENTER);
    row.addView(item,new LinearLayout.LayoutParams(0,-2,1f));
    MaterialButton swatch=new MaterialButton(requireContext());
    int color=ContextCompat.getColor(requireContext(),palette.colorResource);
    swatch.setText(selected?"✓":"");
    swatch.setTextColor(Color.WHITE);
    swatch.setTextSize(16);
    swatch.setAllCaps(false);
    swatch.setGravity(Gravity.CENTER);
    swatch.setInsetTop(0);swatch.setInsetBottom(0);swatch.setPadding(0,0,0,0);
    swatch.setMinHeight(dp(48));swatch.setMinimumHeight(dp(48));
    swatch.setCornerRadius(dp(24));
    swatch.setBackgroundTintList(ColorStateList.valueOf(color));
    swatch.setStrokeWidth(dp(selected?3:0));
    swatch.setStrokeColor(ColorStateList.valueOf(selected?ContextCompat.getColor(requireContext(),R.color.text_primary):color));
    swatch.setContentDescription(palette.label+(selected?" · নির্বাচিত":""));
    LinearLayout.LayoutParams swatchParams=new LinearLayout.LayoutParams(dp(48),dp(48));
    swatchParams.gravity=Gravity.CENTER;
    item.addView(swatch,swatchParams);
    TextView label=new TextView(requireContext());
    label.setText(palette.label);
    label.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));
    label.setTextSize(11);
    label.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(-1,-2);
    labelParams.topMargin=dp(3);
    item.addView(label,labelParams);
    swatch.setOnClickListener(clicked->{
     if(palette.id.equals(PrefsManager.getThemePaletteId()))return;
     if(!ParentControlManager.requireEditAccess(requireContext()))return;
     PrefsManager.setThemePaletteId(palette.id);
     requireActivity().recreate();
    });
   }
  }
 }
 private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
 private void showUpdateDialog(String version,String notes){
  View view=LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_available,null,false);
  TextView versionLabel=view.findViewById(R.id.tvUpdateVersion),current=view.findViewById(R.id.tvUpdateCurrentVersion),releaseNotes=view.findViewById(R.id.tvUpdateNotes),status=view.findViewById(R.id.tvUpdateStatus);
  versionLabel.setText(version.startsWith("v")||version.startsWith("V")?version:"v"+version);
  current.setText("বর্তমান ভার্সন: "+UpdateManager.getCurrentVersionName(requireContext()));
  releaseNotes.setText(notes==null||notes.trim().isEmpty()?"এই আপডেটে নতুন কিছু যোগ করা হয়েছে।":notes.trim());
  AlertDialog dialog=new MaterialAlertDialogBuilder(requireContext()).setView(view).setNegativeButton("পরে",null).setPositiveButton("Download & install",null).create();
  dialog.setOnShowListener(x->{
   android.widget.Button actionButton=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
   actionButton.setOnClickListener(clicked->{actionButton.setEnabled(false);actionButton.setText("Downloading…");status.setVisibility(View.VISIBLE);status.setText("Downloading APK. Please wait…");UpdateManager.downloadAndInstall(requireContext(),null,(ok,msg,url)->{if(!isAdded())return;if(ok){dialog.dismiss();Toast.makeText(requireContext(),msg,Toast.LENGTH_LONG).show();}else{actionButton.setEnabled(true);actionButton.setText("Try again");status.setText(msg);}});});
  });
  dialog.show();
 }
 private void setMode(){if(userCard==null)return;MessageManager.pollLikeResponses(requireContext());boolean dev=PrefsManager.isDevModeEnabled();userCard.setVisibility(dev?View.GONE:View.VISIBLE);devCard.setVisibility(dev?View.VISIBLE:View.GONE);View inbox=requireActivity().findViewById(R.id.btnDeveloperInbox);if(inbox!=null)inbox.setVisibility(dev?View.VISIBLE:View.GONE);}
 private void setupFeedback(View v){attachImage.setOnClickListener(x->pickFeedbackImage());removeImage.setOnClickListener(x->clearFeedbackImage());chipIssue=v.findViewById(R.id.categoryIssue);chipSuggestion=v.findViewById(R.id.categorySuggestion);chipFeedback=v.findViewById(R.id.categoryFeedback);chipIssue.setOnClickListener(x->select("Issue"));chipSuggestion.setOnClickListener(x->select("Suggestion"));chipFeedback.setOnClickListener(x->select("Feedback"));feedback.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){send.setEnabled(!category.isEmpty()&&s.toString().trim().length()>0);}public void afterTextChanged(Editable e){}});send.setOnClickListener(x->sendFeedback());}
 private void select(String c){category=c;chipIssue.setChecked("Issue".equals(c));chipSuggestion.setChecked("Suggestion".equals(c));chipFeedback.setChecked("Feedback".equals(c));send.setEnabled(feedback.getText().toString().trim().length()>0);}
 private void sendFeedback(){String text=feedback.getText().toString().trim();long now=System.currentTimeMillis();if(category.isEmpty()||text.isEmpty())return;if(now-PrefsManager.getLastFeedbackMillis()<30000){Toast.makeText(requireContext(),"৩০ সেকেন্ড পরে আবার পাঠাও।",Toast.LENGTH_SHORT).show();return;}if(PrefsManager.getFeedbackCountToday()>=5){Toast.makeText(requireContext(),"আজ ৫টি মেসেজ পাঠানো হয়েছে।",Toast.LENGTH_SHORT).show();return;}send.setEnabled(false);attachImage.setEnabled(false);MessageManager.submitAnonymous(requireContext(),category,text,selectedFeedbackImage,(ok,msg)->{if(!isAdded())return;send.setEnabled(!category.isEmpty()&&feedback.getText().toString().trim().length()>0);attachImage.setEnabled(true);Toast.makeText(requireContext(),msg,Toast.LENGTH_SHORT).show();if(ok){PrefsManager.recordFeedback(now);feedback.setText("");category="";chipIssue.setChecked(false);chipSuggestion.setChecked(false);chipFeedback.setChecked(false);clearFeedbackImage();}});}
 private void pickFeedbackImage(){Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.setType("image/*");pick.addCategory(Intent.CATEGORY_OPENABLE);pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivityForResult(pick,734);}catch(Exception e){Toast.makeText(requireContext(),"ছবি বাছাই খোলা যায়নি।",Toast.LENGTH_SHORT).show();}}
 private void clearFeedbackImage(){selectedFeedbackImage=null;if(feedbackPreview!=null){feedbackPreview.setImageDrawable(null);feedbackPreview.setVisibility(View.GONE);}if(removeImage!=null)removeImage.setVisibility(View.GONE);if(imageHint!=null)imageHint.setText("ছবি যোগ করো (ঐচ্ছিক)");}
 private void onFeedbackImageSelected(Uri uri){selectedFeedbackImage=uri;feedbackPreview.setImageURI(uri);feedbackPreview.setVisibility(View.VISIBLE);removeImage.setVisibility(View.VISIBLE);imageHint.setText("ছবি সংযুক্ত · সর্বোচ্চ ২২৫ KB-তে compress হবে");}
 private void tapVersion(){long now=System.currentTimeMillis();if(now-firstTap>5000){taps=0;firstTap=now;}taps++;if(taps>=3&&taps<7)Toast.makeText(requireContext(),"ডেভ লগইন খুলতে আর "+(7-taps)+" বার ট্যাপ করো।",Toast.LENGTH_SHORT).show();if(taps>=7){taps=0;startActivityForResult(new Intent(requireContext(),DevModeActivity.class),LOGIN);}}
 
 private void openSettings(Intent i){try{startActivity(i);}catch(Exception e){Toast.makeText(requireContext(),"সেটিংস খোলা যায়নি।",Toast.LENGTH_SHORT).show();}}
 private void openInstagram(){String url="https://instagram.com/pangash.sakib";try{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url));if(i.resolveActivity(requireContext().getPackageManager())!=null){startActivity(i);return;}}catch(Exception ignored){}try{ClipboardManager c=(ClipboardManager)requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);c.setPrimaryClip(ClipData.newPlainText("Instagram","@pangash.sakib"));Toast.makeText(requireContext(),"@pangash.sakib কপি হয়েছে।",Toast.LENGTH_SHORT).show();}catch(Exception ignored){}}
 private void showDebugLog(){android.widget.LinearLayout box=new android.widget.LinearLayout(requireContext());box.setOrientation(1);android.widget.ScrollView scroll=new android.widget.ScrollView(requireContext());TextView text=new TextView(requireContext());text.setText(com.sakib.portebo.overlay.DebugLog.snapshot());text.setTextIsSelectable(true);text.setTextSize(12);text.setPadding(20,16,20,16);scroll.addView(text);box.addView(scroll,new android.widget.LinearLayout.LayoutParams(-1,(int)(320*getResources().getDisplayMetrics().density)));android.widget.LinearLayout row=new android.widget.LinearLayout(requireContext());MaterialButton copy=new MaterialButton(requireContext());copy.setText("কপি");MaterialButton clear=new MaterialButton(requireContext());clear.setText("মুছো");row.addView(copy,new android.widget.LinearLayout.LayoutParams(0,-2,1));row.addView(clear,new android.widget.LinearLayout.LayoutParams(0,-2,1));box.addView(row);androidx.appcompat.app.AlertDialog dialog=new AlertDialog.Builder(requireContext()).setTitle("ডিবাগ লগ").setView(box).setNegativeButton("বন্ধ",null).create();copy.setOnClickListener(v->{android.content.ClipboardManager cm=(android.content.ClipboardManager)requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);cm.setPrimaryClip(android.content.ClipData.newPlainText("Debug log",com.sakib.portebo.overlay.DebugLog.snapshot()));Toast.makeText(requireContext(),"লগ কপি হয়েছে।",Toast.LENGTH_SHORT).show();});clear.setOnClickListener(v->{com.sakib.portebo.overlay.DebugLog.clear();text.setText("");});dialog.show();}
 private void showPublishDialog(){
  View form=LayoutInflater.from(requireContext()).inflate(R.layout.dialog_publish_release,null,false);
  TextInputLayout versionField=form.findViewById(R.id.tilReleaseVersion),titleField=form.findViewById(R.id.tilReleaseTitle),notesField=form.findViewById(R.id.tilReleaseNotes),tokenField=form.findViewById(R.id.tilGithubToken);
  TextInputEditText version=form.findViewById(R.id.etReleaseVersion),title=form.findViewById(R.id.etReleaseTitle),notes=form.findViewById(R.id.etReleaseNotes),token=form.findViewById(R.id.etGithubToken);
  AlertDialog dialog=new MaterialAlertDialogBuilder(requireContext()).setTitle("APK রিলিজ প্রকাশ").setView(form).setNegativeButton("বাতিল",null).setPositiveButton("APK বেছে নাও",null).create();
  dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{versionField.setError(null);titleField.setError(null);tokenField.setError(null);String ver=version.getText()==null?"":version.getText().toString().trim(),name=title.getText()==null?"":title.getText().toString().trim(),body=notes.getText()==null?"":notes.getText().toString().trim(),key=token.getText()==null?"":token.getText().toString().trim();if(ver.isEmpty()){versionField.setError("ভার্সন নম্বর দাও");return;}if(name.isEmpty()){titleField.setError("রিলিজের শিরোনাম দাও");return;}if(key.isEmpty()){tokenField.setError("GitHub token দাও");return;}pendingPublish=new Publish(ver,name,body,key);dialog.dismiss();Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.setType("application/vnd.android.package-archive");pick.addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(pick,732);}catch(Exception e){Toast.makeText(requireContext(),"ফাইল বাছাই খোলা যায়নি।",Toast.LENGTH_SHORT).show();}}));
  dialog.show();
 }
 private Publish pendingPublish;private static class Publish{String v,t,n,k;Publish(String a,String b,String c,String d){v=a;t=b;n=c;k=d;}}
 @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==734&&result==android.app.Activity.RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();try{requireContext().getContentResolver().takePersistableUriPermission(uri,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}onFeedbackImageSelected(uri);return;}if(request==LOGIN&&result==android.app.Activity.RESULT_OK){setMode();return;}if(request==732&&result==android.app.Activity.RESULT_OK&&data!=null&&pendingPublish!=null){android.net.Uri uri=data.getData();if(uri!=null)UpdateManager.publishRelease(requireContext(),uri,pendingPublish.v,pendingPublish.t,pendingPublish.n,pendingPublish.k,(ok,msg,url)->Toast.makeText(requireContext(),msg,Toast.LENGTH_LONG).show());}}
}
