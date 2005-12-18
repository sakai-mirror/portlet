#! /bin/csh

cat << EOF
    <fragment name="Frag.$1" type="page">
        <navigation>
            <title>Sakai $1</title>
            <description>Places a Sakai $1.</description>
        </navigation>
        <fragment name="row" type="row">
           <fragment name="col1" type="column">
               <fragment name="p1" type="portlet">
                   <property name="portlet" value="8.3"/>
               </fragment>
           </fragment>
        </fragment>
    </fragment>

EOF

