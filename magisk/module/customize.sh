for file in "$MODPATH/system/product/overlay/"*
do
  set_perm "$file" root root 644 u:object_r:system_file:s0
done
