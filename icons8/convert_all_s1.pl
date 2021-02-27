#!/usr/bin/perl -w

$TARGET_DIR = "../android/res/";
$TEMP_DIR = "../icons_temp/";

#                      dpi: 120       160       240        320         480            640
my %ic_smaller_sizes  = (ldpi=>20, mdpi=>24, hdpi=>32, xhdpi=>48, xxhdpi=>64,  xxxhdpi=>96);
my %ic_actions_sizes  = (ldpi=>24, mdpi=>32, hdpi=>48, xhdpi=>64, xxhdpi=>96,  xxxhdpi=>128);
my %ic_menu_sizes     = (ldpi=>36, mdpi=>48, hdpi=>72, xhdpi=>96, xxhdpi=>144, xxxhdpi=>192);
my %ic_launcher_sizes = (ldpi=>36, mdpi=>48, hdpi=>72, xhdpi=>96, xxhdpi=>144, xxxhdpi=>192);
my %ic_bigicons_sizes = (ldpi=>36, mdpi=>48, hdpi=>72, xhdpi=>96, xxhdpi=>144, xxxhdpi=>192);

my %ic_smaller_list=(
        'icons8_minus_minus_small.svg' => 'icons8_minus_minus_small.png',
        'icons8_minus_small.svg' => 'icons8_minus_small.png',
        'icons8_plus_plus_small.svg' => 'icons8_plus_plus_small.png',
        'icons8_plus_small.svg' => 'icons8_plus_small.png',
        'icons8_drop_up_no_frame.svg' => 'icons8_drop_up_no_frame_small.png',
        'icons8_unchecked_checkbox.svg' => 'icons8_unchecked_checkbox.png',
        'icons8_checked_checkbox.svg' => 'icons8_checked_checkbox.png',
        'icons8_check_no_frame.svg' => 'icons8_check_no_frame.png',
        'icons8_delete.svg' => 'icons8_delete.png'
);

my %ic_actions_list=(
        'icons8_hide.svg' => 'icons8_hide.png',
        'icons8_library.svg' => 'icons8_library.png',
        'icons8_bookmark_plus_q.svg' => 'icons8_bookmark_plus_q.png',
        'icons8_two_fingers.svg' => 'icons8_two_fingers.png',
        'icons8_select_all.svg' => 'icons8_select_all.png',
        'cr3_option_text_multilang.svg' => 'cr3_option_text_multilang.png',
        'icons8_skim.svg' => 'icons8_skim.png',
        'icons8_combo.svg' => 'icons8_combo.png',
        'icons8_super_combo.svg' => 'icons8_super_combo.png',
        'icons8_web_search.svg' => 'icons8_web_search.png',
        'icons8_night_vision.svg' => 'icons8_night_vision.png',
        'icons8_sun_auto.svg' => 'icons8_sun_auto.png',
        'icons8_sun_warm.svg' => 'icons8_sun_warm.png',
        'icons8_sun_cold.svg' => 'icons8_sun_cold.png',
        'icons8_page_animation_speed.svg' => 'icons8_page_animation_speed.png',
        'icons8_page_animation.svg' => 'icons8_page_animation.png',
        'icons8_opds.svg' => 'icons8_opds.png',
        'icons8_calibre.svg' => 'icons8_calibre.png',
        'litres_en_logo_2lines.svg' => 'icons8_litres_en_logo_2lines_big.png'
);

my %ic_menu_list=(
        'icons8_css.svg' => 'icons8_css.png'
);

my %ic_launcher_list=(
);

my %ic_bigicons_list=(
);

my ($srcfile, $dstfile);
my ($dpi, $size);
my $folder;
my $resfile;
my $cmd;
my $ret;

# smaller icons
while (($srcfile, $dstfile) = each(%ic_smaller_list))
{
	while (($dpi, $size) = each(%ic_smaller_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z --export-filename ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# action icons
while (($srcfile, $dstfile) = each(%ic_actions_list))
{
	while (($dpi, $size) = each(%ic_actions_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z --export-filename ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# menu icons
while (($srcfile, $dstfile) = each(%ic_menu_list))
{
	while (($dpi, $size) = each(%ic_menu_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z --export-filename ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

		}
	}
}

# launcher icons
while (($srcfile, $dstfile) = each(%ic_launcher_list))
{
	while (($dpi, $size) = each(%ic_launcher_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z --export-filename ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

		}
	}
}

# bigicons
while (($srcfile, $dstfile) = each(%ic_bigicons_list))
{
	while (($dpi, $size) = each(%ic_bigicons_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z --export-filename ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

		}
	}
}