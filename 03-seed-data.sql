-- Synthetic Property Listings: 1000 random properties listed at 20% above last sale price
INSERT INTO property_listings (property_id, listing_date, price)
SELECT property_id, CURRENT_DATE, ROUND(purchase_price * 1.20)
FROM (
    SELECT DISTINCT ON (property_id) property_id, purchase_price
    FROM properties
    WHERE purchase_price IS NOT NULL AND purchase_price > 0
    ORDER BY property_id, settlement_date DESC NULLS LAST
) latest
ORDER BY RANDOM()
LIMIT 1000;

-- Synthetic Purchasers: 10,000 accounts with random names
DO $$
DECLARE
    first_names TEXT[] := ARRAY[
        'James','Mary','John','Patricia','Robert','Jennifer','Michael','Linda',
        'William','Elizabeth','David','Barbara','Richard','Susan','Joseph','Jessica',
        'Thomas','Sarah','Christopher','Karen','Charles','Nancy','Daniel','Lisa',
        'Matthew','Betty','Anthony','Margaret','Mark','Sandra','Donald','Ashley',
        'Steven','Kimberly','Paul','Emily','Andrew','Donna','Joshua','Michelle',
        'Kenneth','Carol','Kevin','Amanda','Brian','Dorothy','George','Melissa',
        'Timothy','Deborah','Ronald','Stephanie','Edward','Rebecca','Jason','Sharon',
        'Jeffrey','Laura','Ryan','Cynthia','Jacob','Kathleen','Gary','Amy',
        'Nicholas','Angela','Eric','Shirley','Jonathan','Anna','Stephen','Brenda',
        'Larry','Pamela','Justin','Emma','Scott','Nicole','Brandon','Helen',
        'Benjamin','Samantha','Samuel','Katherine','Raymond','Christine','Gregory','Debra',
        'Alexander','Rachel','Patrick','Carolyn','Frank','Janet','Jack','Catherine',
        'Dennis','Maria','Jerry','Heather','Tyler','Diane','Aaron','Ruth'
    ];
    last_names TEXT[] := ARRAY[
        'Smith','Johnson','Williams','Brown','Jones','Garcia','Miller','Davis',
        'Rodriguez','Martinez','Hernandez','Lopez','Gonzalez','Wilson','Anderson','Thomas',
        'Taylor','Moore','Jackson','Martin','Lee','Perez','Thompson','White',
        'Harris','Sanchez','Clark','Ramirez','Lewis','Robinson','Walker','Young',
        'Allen','King','Wright','Scott','Torres','Nguyen','Hill','Flores',
        'Green','Adams','Nelson','Baker','Hall','Rivera','Campbell','Mitchell',
        'Carter','Roberts','Gomez','Phillips','Evans','Turner','Diaz','Parker',
        'Cruz','Edwards','Collins','Reyes','Stewart','Morris','Morales','Murphy',
        'Cook','Rogers','Gutierrez','Ortiz','Morgan','Cooper','Peterson','Bailey',
        'Reed','Kelly','Howard','Ramos','Kim','Cox','Ward','Richardson',
        'Watson','Brooks','Chavez','Wood','James','Bennett','Gray','Mendoza',
        'Ruiz','Hughes','Price','Alvarez','Castillo','Sanders','Patel','Myers',
        'Long','Ross','Foster','Jimenez','Powell','Jenkins','Perry','Russell'
    ];
    fname TEXT;
    lname TEXT;
    purchaser_id_val INT;
    num_interests INT;
    postcodes TEXT[];
    pc TEXT;
    selected TEXT[];
    idx INT;
BEGIN
    -- Collect distinct postcodes from properties
    SELECT ARRAY_AGG(DISTINCT post_code)
    INTO postcodes
    FROM properties
    WHERE post_code IS NOT NULL;

    IF postcodes IS NULL OR array_length(postcodes, 1) IS NULL THEN
        RAISE NOTICE 'No postcodes found — skipping purchaser generation.';
        RETURN;
    END IF;

    FOR i IN 1..10000 LOOP
        fname := first_names[1 + floor(random() * array_length(first_names, 1))::int];
        lname := last_names[1 + floor(random() * array_length(last_names, 1))::int];

        INSERT INTO purchasers (first_name, last_name, email, phone)
        VALUES (
            fname,
            lname,
            lower(fname) || '.' || lower(lname) || '.' || substr(gen_random_uuid()::text, 1, 8) || '@example.com',
            '04' || lpad(floor(random() * 100000000)::text, 8, '0')
        )
        RETURNING purchasers.purchaser_id INTO purchaser_id_val;

        -- 0 to 5 random postcode interests
        num_interests := floor(random() * 6)::int;
        selected := ARRAY[]::TEXT[];

        FOR j IN 1..num_interests LOOP
            idx := 1 + floor(random() * array_length(postcodes, 1))::int;
            pc := postcodes[idx];
            IF NOT pc = ANY(selected) THEN
                selected := array_append(selected, pc);
                INSERT INTO purchaser_interests (purchaser_id, post_code)
                VALUES (purchaser_id_val, pc)
                ON CONFLICT DO NOTHING;
            END IF;
        END LOOP;
    END LOOP;
END $$;
